package ca.bc.gov.hlth.hl7v2plugin.connection;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslContextConfigurator;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.glassfish.tyrus.client.auth.Credentials;
import org.glassfish.tyrus.core.TyrusSession;
import org.glassfish.tyrus.core.WebSocketException;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.SoapUISystemProperties;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.settings.SSLSettings;
import com.eviware.soapui.support.StringUtils;

import ca.bc.gov.hlth.hl7v2plugin.PluginConfig;
import ca.bc.gov.hlth.hl7v2plugin.hl7xfer.HL7XferException;
import ca.bc.gov.hlth.hl7v2plugin.hl7xfer.HL7XferMessageSender;

public class TyrusClient extends Endpoint implements Client {

    private final static Logger LOGGER = Logger.getLogger(PluginConfig.LOGGER_NAME);

    private AtomicReference<Session> session = new AtomicReference<Session>();
    private AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
    private BlockingQueue<Message<?>> messageQueue = new LinkedBlockingQueue<Message<?>>();
    private AtomicReference<Future<?>> future = new AtomicReference<Future<?>>();

    private ClientEndpointConfig cec;
    private ClientManager client;
    private URI uri;
    private ProxySelector proxySelector;
    private boolean proxySelectorWorkaround;
    private volatile boolean disposing = false;
    private volatile boolean connecting = false;

    public TyrusClient(ExpandedConnectionParams connectionParams) throws URISyntaxException {

        ClientEndpointConfig.Builder builder = ClientEndpointConfig.Builder.create();

        if (connectionParams.hasSubprotocols()) {
            builder.preferredSubprotocols(Arrays.asList(connectionParams.subprotocols.split(",")));
        }
        cec = builder.build();

        ClientManager client = ClientManager
                .createClient("org.glassfish.tyrus.container.jdk.client.JdkClientContainer");
        client.setDefaultMaxSessionIdleTimeout(-1);

        client.getProperties().put(ClientProperties.REDIRECT_ENABLED, Boolean.TRUE);
        if (LOGGER.isTraceEnabled()) {
            client.getProperties().put(ClientProperties.LOG_HTTP_UPGRADE, Boolean.TRUE);
        }

        if (connectionParams.hasCredentials()) {
            client.getProperties().put(
                    ClientProperties.CREDENTIALS,
                    new Credentials(connectionParams.login, connectionParams.password == null ? ""
                                    : connectionParams.password));
        }

        Settings settings = SoapUI.getSettings();

        String keyStoreUrl = System.getProperty(SoapUISystemProperties.SOAPUI_SSL_KEYSTORE_LOCATION,
                settings.getString(SSLSettings.KEYSTORE, null));

        String pass = System.getProperty(SoapUISystemProperties.SOAPUI_SSL_KEYSTORE_PASSWORD,
                settings.getString(SSLSettings.KEYSTORE_PASSWORD, ""));

        if (!StringUtils.isNullOrEmpty(keyStoreUrl)) {
            SslContextConfigurator sslContextConfigurator = new SslContextConfigurator(true);
            sslContextConfigurator.setKeyStoreFile(keyStoreUrl);
            sslContextConfigurator.setKeyStorePassword(pass);
            sslContextConfigurator.setKeyStoreType("JKS");
            if (sslContextConfigurator.validateConfiguration()) {
                SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(sslContextConfigurator, true,
                        false, false);
                client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
            } else {
                LOGGER.warn("error validating keystore configuration");
            }
        }

        this.client = client;

        uri = new URI(connectionParams.getNormalizedServerUri());
    }

    /**
     *
     * @see ca.bc.gov.hlth.hl7v2plugin.connection.Client#cancel()
     */
    @Override
    public void cancel() {
        Future<?> future;
        if ((future = this.future.get()) != null) {
            future.cancel(true);
        }
    }

    /**
     *
     * @see ca.bc.gov.hlth.hl7v2plugin.connection.Client#connect(long)
     */
    @Override
    public void connect(long timeoutMillis) {
        synchronized (this) {
            if (isConnected() || connecting) {
                return;
            }
            connecting = true;
            disposing = false;
        }

        try {

            checkProxySelector();

            throwable.set(null);
            future.set(null);

            if (timeoutMillis <= 0) {
                client.getProperties().put(ClientProperties.HANDSHAKE_TIMEOUT, Integer.MAX_VALUE);
            } else {
                client.getProperties().put(ClientProperties.HANDSHAKE_TIMEOUT, (int) timeoutMillis);
            }

            Future<Session> future = client.asyncConnectToServer(this, cec, uri);
            this.future.set(future);

        } catch (Exception e) {
            Throwable th = ExceptionUtils.getRootCause(e);
            throwable.set(th != null ? th : e);
            SoapUI.logError(th != null ? th : e);
        }
    }

    // FIXME: workaround https://java.net/jira/browse/TYRUS-412
    public void checkProxySelector() {
        proxySelector = ProxySelector.getDefault();
        if (proxySelector == null) {
            proxySelectorWorkaround = true;
            //ProxySelector.setDefault(NoProxySelector.getInstance());
        }
    }

    /**
     *
     * @see ca.bc.gov.hlth.hl7v2plugin.connection.Client#disconnect(boolean)
     */
    @Override
    public void disconnect(boolean harshDisconnect) throws Exception {
        Session session;
        if ((session = this.session.get()) != null) {
            if (!harshDisconnect) {
                session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "drop connection test step"));
            } else {
                session.close(new CloseReason(CloseCodes.PROTOCOL_ERROR, "drop connection test step"));
                this.session.set(null);
            }
        }
    }

    /**
     *
     * @see ca.bc.gov.hlth.hl7v2plugin.connection.Client#dispose()
     */
    @Override
    public void dispose() {
        disposing = true;

        resetProxySelector();

        Session session;
        if ((session = this.session.get()) != null) {
            try {
                session.close();
            } catch (IOException e) {
                LOGGER.warn("Couldn't close session", e);
            }
        }
        this.session.set(null);
        throwable.set(null);
        future.set(null);
    }

    /**
     *
     * @see ca.bc.gov.hlth.hl7v2plugin.connection.Client#nextMessage(long)
     */
    @Override
    public Message<?> nextMessage(long timeoutMillis) {
        try {
            return messageQueue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     *
     * @see ca.bc.gov.hlth.hl7v2plugin.connection.Client#getThrowable()
     */
    @Override
    public Throwable getThrowable() {
        return throwable.get();
    }

    /**
     *
     * @see ca.bc.gov.hlth.hl7v2plugin.connection.Client#isAvailable()
     */
    @Override
    public boolean isAvailable() {
        Future<?> future;
        if ((future = this.future.get()) != null) {
            if (future.isDone()) {
                try {
                    future.get();
                    return true;
                } catch (Exception e) {
                    throwable.set(e);
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     *
     * @see ca.bc.gov.hlth.hl7v2plugin.connection.Client#isConnected()
     */
    @Override
    public boolean isConnected() {
        Session session;
        if ((session = this.session.get()) != null) {
            return session.isOpen();
        } else {
            return false;
        }
    }

    /**
     *
     * @see ca.bc.gov.hlth.hl7v2plugin.connection.Client#isFaulty()
     */
    @Override
    public boolean isFaulty() {
        return throwable.get() != null;
    }

    @Override
    public void onClose(Session session, final CloseReason closeReason) {
        SoapUI.log("WebSocketClose statusCode=" + closeReason.getCloseCode() + " reason="
                + closeReason.getReasonPhrase());
        messageQueue.clear();

        resetProxySelector();

        this.session.set(null);

        Future<?> future;
        if (closeReason.getCloseCode().getCode() > CloseCodes.NORMAL_CLOSURE.getCode()) {
            throwable.set(websocketException("Websocket connection closed abnormaly.", closeReason));
        } else if ((future = this.future.get()) != null && !future.isDone()) {
            throwable.set(websocketException("Websocket connection closed unexpected.", closeReason));
        }
    }

    public WebSocketException websocketException(final String message, final CloseReason closeReason) {
        return new WebSocketException(message) {

            @Override
            public CloseReason getCloseReason() {
                return closeReason;
            }

            @Override
            public String toString() {
                return getMessage()
                        + " ["
                        + closeReason.getCloseCode().getCode()
                        + "] "
                        + closeReason.getCloseCode()
                        + (StringUtils.hasContent(closeReason.getReasonPhrase()) ? " '" + closeReason.getReasonPhrase()
                                + "' " : "");
            }
        };
    }

    // FIXME: workaround https://java.net/jira/browse/TYRUS-412
    public void resetProxySelector() {
        if (proxySelectorWorkaround) {
            ProxySelector.setDefault(proxySelector);
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        SoapUI.log("WebSocketConnect success=" + session.isOpen() + " accepted protocol="
                + session.getNegotiatedSubprotocol());
        resetProxySelector();

        this.session.set(session);

        connecting = false;

        if (disposing) {
            SoapUI.log("WebSocketClient already disposed closing session");
            dispose();
            return;
        }

        ((TyrusSession) session).setHeartbeatInterval(30000);

        session.addMessageHandler(new MessageHandler.Whole<String>() {

            @Override
            public void onMessage(String payload) {
                Message.TextMessage message = new Message.TextMessage(payload);
                if (!messageQueue.offer(message)) {
                    SoapUI.log("Internal queue overloaded, closing session.");
                }
            }
        });
        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {

            @Override
            public void onMessage(ByteBuffer payload) {
                Message.BinaryMessage message = new Message.BinaryMessage(payload);
                if (!messageQueue.offer(message)) {
                    SoapUI.log("Internal queue overloaded, closing session.");
                }
            }
        });

    }

    @Override
    public void onError(Session session, Throwable cause) {
        SoapUI.logError(cause, "WebSocketError");

        resetProxySelector();

        throwable.set(cause);

        connecting = false;
    }

    /**
     *
     * @see
     * ca.bc.gov.hlth.hl7v2plugin.connection.Client#sendMessage(ca.bc.gov.hlth.hl7v2plugin.connection.Message,long)
     */
    @Override
    public void sendMessage(Message<?> message, long timeoutMillis) {
        Session session;
        if ((session = this.session.get()) != null) {
            throwable.set(null);
            future.set(null);

            Async asyncRemote = session.getAsyncRemote();
            asyncRemote.setSendTimeout(75000);

            if (message instanceof Message.TextMessage) {
                Message.TextMessage text = (Message.TextMessage) message;

                try {
                    HL7XferMessageSender messageSender = new HL7XferMessageSender(19430, 75, "127.0.0.1");
                    String respMsg = messageSender.send(text.getPayload());
                    Scanner s = new Scanner(respMsg);
                    while (s.hasNextLine()) {
                        System.out.println(s.nextLine());
                    }
                } catch (HL7XferException ex) {
                    System.out.println("Exception:" + ex.getMessage());
                }

                //future.set(sendMsgToHNClient(text.getPayload()));
            }
            if (message instanceof Message.BinaryMessage) {
                Message.BinaryMessage binary = (Message.BinaryMessage) message;
                future.set(asyncRemote.sendBinary(binary.getPayload()));
            }
        }
    }

    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    public Future<String> sendMsgToHNClient(final String msg) {
        return pool.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String respMsg = null;
                
                try {
                    HL7XferMessageSender messageSender = new HL7XferMessageSender(19430, 75, "127.0.0.1");
                    respMsg = messageSender.send(msg);
                    Scanner s = new Scanner(respMsg);
                    while (s.hasNextLine()) {
                        System.out.println(s.nextLine());
                    }
                    
                    
                    
                } catch (HL7XferException ex) {
                    System.out.println("Exception:" + ex.getMessage());
                }
                
                return respMsg; 
            }
        });
    }
}
