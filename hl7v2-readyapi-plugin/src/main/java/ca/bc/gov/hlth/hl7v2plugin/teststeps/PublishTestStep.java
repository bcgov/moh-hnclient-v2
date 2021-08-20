package ca.bc.gov.hlth.hl7v2plugin.teststeps;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.ServiceConfig;
import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
import com.eviware.soapui.impl.wsdl.support.AbstractNonHttpMessageExchange;
import com.eviware.soapui.impl.wsdl.support.IconAnimator;
import com.eviware.soapui.impl.wsdl.support.assertions.AssertableConfig;
import com.eviware.soapui.impl.wsdl.support.assertions.AssertionsSupport;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequest;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.TestAssertionRegistry;
import com.eviware.soapui.model.environment.Endpoint;
import com.eviware.soapui.model.environment.Environment;
import com.eviware.soapui.model.environment.Service;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.iface.Response;
import com.eviware.soapui.model.iface.Submit;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.iface.SubmitListener;
import com.eviware.soapui.model.mock.MockRunner;
import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionsListener;
import com.eviware.soapui.model.testsuite.LoadTestRunner;
import com.eviware.soapui.model.testsuite.ProjectRunner;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.model.testsuite.TestSuiteRunner;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.monitor.TestMonitorListener;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.security.SecurityTestRunner;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.smartbear.ready.core.ApplicationEnvironment;
import com.smartbear.ready.core.RuntimeEnvironment;

import ca.bc.gov.hlth.hl7v2plugin.CancellationToken;
import ca.bc.gov.hlth.hl7v2plugin.PluginConfig;
import ca.bc.gov.hlth.hl7v2plugin.Utils;
import ca.bc.gov.hlth.hl7v2plugin.XmlObjectBuilder;
import ca.bc.gov.hlth.hl7v2plugin.connection.Client;
import ca.bc.gov.hlth.hl7v2plugin.connection.Message;
import ca.bc.gov.hlth.hl7v2plugin.hl7xfer.HL7XferException;
import ca.bc.gov.hlth.hl7v2plugin.hl7xfer.HL7XferMessageSender;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.actions.groups.PublishTestStepActionGroup;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.panels.ConnectedTestStepPanel;

@PluginTestStep(typeName = "hl7v2PublishTestStep", name = "Publish HL7v2 to HNClient",
        description = "Publishes a specified message through the given HNClient.",
        iconPath = "ca/bc/gov/hlth/hl7v2plugin/hl7_request_step.png")
public class PublishTestStep extends ConnectedTestStep implements Assertable, TestMonitorListener, PropertyChangeListener {

    private final static Logger logger = LoggerFactory.getLogger(PluginConfig.LOGGER_NAME);

    private final static String MESSAGE_KIND_SETTING_NAME = "MessageKind";
    private final static String MESSAGE_SETTING_NAME = "Message";
    private final static String MESSAGE_TYPE_PROP_NAME = "MessageType";
    private final static String MESSAGE_PROP_NAME = "Message";
    private final static String RECEIVED_MESSAGE_PROP_NAME = "Response";
    private final static String ASSERTION_SECTION = "assertion";
    
    private static final String PORT_SEPARATOR = ":";
    
    private static final String API_NAME = "HNS Client";
    
    private static final String PROPERTY_SENDING_FACILITY = "sendingFacility";
    
    private static final String ENDPOINT_UNDEFINED = "Undefined";
   
    public final static PublishedMessageType DEFAULT_MESSAGE_TYPE = PublishedMessageType.BinaryFile;

    private MessageType expectedMessageType = MessageType.Text;

    private static boolean actionGroupAdded = false;
    private PublishedMessageType messageKind = DEFAULT_MESSAGE_TYPE;

    /** UI content */
    private String message;
    private String response;

    private AssertionsSupport assertionsSupport;
    private Assertable.AssertionStatus assertionStatus = Assertable.AssertionStatus.UNKNOWN;
    private ArrayList<TestAssertionConfig> assertionConfigs = new ArrayList<TestAssertionConfig>();
    
    /** Icons */
    private ImageIcon disabledStepIcon;
    
    private ImageIcon unknownStepIcon;
    private PublishIconAnimator<?> iconAnimator;

    private MessageExchangeImpl messageExchange;
    
    private Set<SubmitListener> submitListeners = new HashSet<>();
    
    private PropertyChangeNotifier notifier;

    public PublishTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
        super(testCase, config, true, forLoadTest);
        if (!actionGroupAdded) {
            SoapUI.getActionRegistry().addActionGroup(new PublishTestStepActionGroup());
            actionGroupAdded = true;
        }
        if (config != null && config.getConfig() != null) {
            XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(config.getConfig());
            readData(reader);
        }

        initAssertions(config);

        addProperty(new DefaultTestStepProperty(MESSAGE_TYPE_PROP_NAME, false,
                new DefaultTestStepProperty.PropertyHandler() {
                    @Override
                    public String getValue(DefaultTestStepProperty property) {
                        return messageKind.toString();
                    }

                    @Override
                    public void setValue(DefaultTestStepProperty property, String value) {
                        PublishedMessageType messageType = PublishedMessageType.fromString(value);
                        if (messageType != null) {
                            setMessageKind(messageType);
                        }
                    }
                }, this));
        addProperty(new TestStepBeanProperty(MESSAGE_PROP_NAME, false, this, "message", this));

        addProperty(new TestStepBeanProperty(RECEIVED_MESSAGE_PROP_NAME, true, this, "response", this));

        addProperty(new DefaultTestStepProperty(TIMEOUT_PROP_NAME, false,
                new DefaultTestStepProperty.PropertyHandler() {
                    @Override
                    public String getValue(DefaultTestStepProperty property) {
                        return Integer.toString(getTimeout());
                    }

                    @Override
                    public void setValue(DefaultTestStepProperty property, String value) {
                        int newTimeout;
                        try {
                            newTimeout = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            return;
                        }
                        setTimeout(newTimeout);
                    }

                }, this));

        if (!forLoadTest) {
            initIcons();
        }
        //setIcon(unknownStepIcon);
        
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null) {
            testMonitor.addTestMonitorListener(this);
        }

        updateState();
    }
    
	private PropertyChangeNotifier getNotifier() {
		if (this.notifier == null) {
			this.notifier = new PropertyChangeNotifier();
		}
		return this.notifier;
	}

    private boolean checkProperties(WsdlTestStepResult result, PublishedMessageType messageTypeToCheck,
            String messageToCheck) {
    	logger.info("checkProperties");
        boolean ok = true;
        if (messageTypeToCheck == null) {
            result.addMessage("The message format is not specified.");
            ok = false;
        }
        if (StringUtils.isNullOrEmpty(messageToCheck) && messageTypeToCheck != PublishedMessageType.Text) {
            if (messageTypeToCheck == PublishedMessageType.BinaryFile) {
                result.addMessage("A file which contains a message is not specified");
            } else {
                result.addMessage("A message content is not specified.");
            }
            ok = false;
        }
        logger.info("checkProperties result {}", result.getMessages());
        return ok;
    }

    @Override
    protected ExecutableTestStepResult doExecute(SubmitContext testRunContext, CancellationToken cancellationToken) {
    	logger.info("doExecute");
        ExecutableTestStepResult result = new ExecutableTestStepResult(this);
        result.startTimer();
        result.setStatus(TestStepResult.TestStepStatus.UNKNOWN);
        if (iconAnimator != null) {
        	logger.trace("Starting icon animator");
            iconAnimator.start();
        }
        try {
        	Environment activeEnvironment = getActiveEnvironment();
        	if (activeEnvironment == null) {
        		logger.warn("No environment selected");
        		result.setStatus(TestStepResult.TestStepStatus.FAILED);
        		return result;
        	}
            if (!checkProperties(result, messageKind, message)) {
            	logger.info("Reporting error");
                return reportError(result, null, cancellationToken);
            }

            String msgOrFolderPath = testRunContext.expand(message);

        	Integer timeout = getTimeout();
            String endpoint = getHNSClientEndpoint(activeEnvironment);
            String serverUri = org.apache.commons.lang3.StringUtils.substringBefore(endpoint, PORT_SEPARATOR);
            Integer port = Integer.valueOf(org.apache.commons.lang3.StringUtils.substringAfter(endpoint, PORT_SEPARATOR));
            		
            		
            logger.info("Server: {}, Port: {}", serverUri, port);
            // If the message is a folder path
            if (PublishedMessageType.BinaryFile.equals(messageKind)) {
                //TODO parse folder and send messages
                final File folder = new File(msgOrFolderPath);
                List<File> files = listFilesForFolder(folder);

                String resp;

                try {
                    //Sending the message and getting the response immediately
                    for (File f : files) {
                        String msg = extractMessageFromFile(f);
                        result.addMessage(msg);
                        
                        HL7XferMessageSender messageSender = new HL7XferMessageSender(port, 75, serverUri);
                        resp = messageSender.send(msg);
                        writeRepsonseInOutputFile(msg, resp, f);
                    }
                    
                    setResponse("All the messages were sent and the responses were written in the corresponding output files.");
                    result.setStatus(TestStepResult.TestStepStatus.OK);
                    
                } catch (HL7XferException ex) {
                    logger.info("Exception thrown when sending message to HNClient: " + ex);
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                }

            } else { //The message is the actual message

            	String request = msgOrFolderPath;
                if (!StringUtils.isBlank(request)) {
                	//request = request.replaceAll("\\n", "\r");
                }
                
                if (StringUtils.isBlank(request)) {
                	result.setStatus(TestStepResult.TestStepStatus.FAILED);
                	throw new Exception();
                }
                
                logger.info("Request was " + request);
                
                // Check for ReadyAPI properties to perform variable replacement
                // Just support specific matches for now
                request = performPropertyReplacement(testRunContext, request);
                
                logger.info("Expanded Message Sent: " + request);

                String resp;
                boolean failed = false;

                try {
                    // Sending the message and getting the response immediately
                	
                    logger.info("Timeout {}", getTimeout());
                	
                    HL7XferMessageSender messageSender = new HL7XferMessageSender(port, 99999, serverUri);
                    resp = messageSender.send(request);
                    setResponse(resp);
                    result.addMessage("Response: " + resp);
                    logger.info("Response {}", response);

                    //Checking assertions
                    for (WsdlMessageAssertion assertion : assertionsSupport.getAssertionList()) {
                    	logger.info("Checking assertion {} ", assertion.getDescription());
                        //applyAssertion(assertion, testRunContext);
                       // failed = assertion.isFailed();
                    }
                    
                    // TODO Use the common assertResponse method
                    for (WsdlMessageAssertion assertion : assertionsSupport.getAssertionList()) {
                        applyAssertion(assertion, testRunContext);
                        AssertionError[] errors = assertion.getErrors();
                        if (errors != null) {
                            for (AssertionError error : errors) {
                                result.addMessage("[" + assertion.getName() + "] " + error.getMessage());
                            }
                        }
                    }
                    logger.info("assertions complete status {} " + getAssertionStatus());
                    getNotifier().notifyChange();
                    logger.info("notified");
                    

//                    if (resp == null || failed) {
//                        if (cancellationToken.isCancelled()) {
//                            result.setStatus(TestStepResult.TestStepStatus.CANCELED);
//                        } else {
//                            result.addMessage("The test step's timeout has expired");
//                            result.setStatus(TestStepResult.TestStepStatus.FAILED);
//                        }
//                    } else {
//                        result.setStatus(TestStepResult.TestStepStatus.OK);
//                    }
                    
                    switch (getAssertionStatus()) {
//                    case null:
//                      result.setStatus(TestStepResult.TestStepStatus.FAILED);
//                      break;
                    case VALID:
                      result.setStatus(TestStepResult.TestStepStatus.OK);
                      break;
                    case UNKNOWN:
                      result.setStatus(TestStepResult.TestStepStatus.UNKNOWN);
                      break;
					case FAILED:
						result.setStatus(TestStepResult.TestStepStatus.FAILED);
						break;
					case WARNINGS:
						break;
					default:
						break;
                  } 

                } catch (HL7XferException ex) {
                    logger.error("Exception thrown when sending message to HNClient", ex);
                    result.addMessage(ex.getErrorMessage());
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                }
            }

        } catch (Exception e) {
        	logger.info(e.getMessage(), e);
            result.setStatus(TestStepResult.TestStepStatus.FAILED);
            result.setError(e);
        } finally {
            result.stopTimer();
            if (iconAnimator != null) {
                iconAnimator.stop();
            }
            updateState();
            result.setOutcome(formOutcome(result));
            SoapUI.log(String.format("%s - [%s test step]", result.getOutcome(), getName()));
            notifyExecutionListeners(result);
        }

        return result;
    }
    
    private String performPropertyReplacement(SubmitContext testRunContext, String message) {
        // Check for ReadyAPI properties to perform variable replacement
        // Just support specific matches for now
        // sendingFacility
        if (message.contains(PROPERTY_SENDING_FACILITY)) {
        	String sendingFacility = getProject().getProperty(PROPERTY_SENDING_FACILITY).getValue();
        	return message.replace(PROPERTY_SENDING_FACILITY, sendingFacility);	
        }
        return message;
    }
    
    protected TestStepResult.TestStepStatus getFailedResultBasedOnRunContext(Submit submit) {
        if (isForLoadTest() && submit != null && submit.getStatus() == Submit.Status.CANCELED)
          return TestStepResult.TestStepStatus.CANCELED; 
        return TestStepResult.TestStepStatus.FAILED;
      }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
    	logger.debug("propertyChange {} ", event.getPropertyName());
        if (event.getPropertyName().equals(TestAssertion.CONFIGURATION_PROPERTY)
                || event.getPropertyName().equals(TestAssertion.DISABLED_PROPERTY)) {
            updateData();
            assertReceivedMessage();
        }
    }
    
    /**
     * This method makes sure that we're only dealing with files and not folders
     *
     * @param folder
     * @return
     */
    public List<File> listFilesForFolder(final File folder) {
        final List<File> files = new ArrayList<>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                files.addAll(listFilesForFolder(fileEntry));
            } else {
                if (!fileEntry.getName().contains("_out") && fileEntry.getName().endsWith(".txt")) {
                    files.add(fileEntry);
                }
            }
        }

        return files;
    }

    /**
     * This method writes the original message and response in an output file At
     * the same location as the input file
     *
     * @param msg
     * @param resp
     * @param f
     * @throws IOException
     */
    private void writeRepsonseInOutputFile(String msg, String resp, File f) throws IOException {
        List<String> lines = Arrays.asList("Message sent:", msg, "", "", "Response received:", resp);
        int idx = f.getAbsolutePath().lastIndexOf(".txt");
        String newPath = new StringBuilder(f.getAbsolutePath()).insert(idx, "_out").toString();
        Path file = Paths.get(newPath);
        Files.write(file, lines, Charset.forName("UTF-8"));
    }

    /**
     * This method reads an input file and extract the message from it
     *
     * @param f
     * @return
     * @throws IOException
     */
    private String extractMessageFromFile(File f) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
        String message = new String(encoded, StandardCharsets.UTF_8);
        message = message.replaceAll("\\n", "\r");
        return message;
    }

    private String formOutcome(WsdlTestStepResult executionResult) {
        switch (executionResult.getStatus()) {
            case CANCELED:
                return "CANCELED";
            case FAILED:
                if (executionResult.getError() == null) {
                    return "Unable to publish the message (" + StringUtils.join(executionResult.getMessages(), " ") + ")";
                } else {
                    return "Error during message publishing: " + Utils.getExceptionMessage(executionResult.getError());
                }
            default:
                return String.format("The message has been published within %d ms", executionResult.getTimeTaken());

        }
    }

    public String getMessage() {
        return message;
    }

    public String getResponse() {
        return response;
    }

    public PublishedMessageType getMessageKind() {
        return messageKind;
    }

    protected void initIcons() {
    	
        //unknownStepIcon = UISupport.createImageIcon("com/tsystems/readyapi/plugin/websocket/unknown_publish_step.png");
        //disabledStepIcon = UISupport.createImageIcon("com/tsystems/readyapi/plugin/websocket/disabled_publish_step.png");
        
    	disabledStepIcon = UISupport.createImageIcon("ca/bc/gov/hlth/hl7v2plugin/disabled_hl7_request_step.png");

        unknownStepIcon = UISupport.createImageIcon("ca/bc/gov/hlth/hl7v2plugin/unknown_hl7_request_step.png");


//        iconAnimator = new IconAnimator<PublishTestStep>(this, "ca/bc/gov/hlth/hl7v2plugin/unknown_publish_step.png",
//                "ca/bc/gov/hlth/hl7v2plugin/publish_step.png", 4);
        
//        iconAnimator = new IconAnimator<PublishTestStep>(this, "com/tsystems/readyapi/plugin/websocket/unknown_publish_step.png",
//                "com/tsystems/readyapi/plugin/websocket/publish_step.png", 5);
        
        //setIconAnimator(new PublishIconAnimator(this, "com/smartbear/mqttsupport/unknown_publish_step.png", "com/smartbear/mqttsupport/publish_step.png", 5));
        setIconAnimator(new PublishIconAnimator(this, "ca/bc/gov/hlth/hl7v2plugin/unknown_publish_step.png", "ca/bc/gov/hlth/hl7v2plugin/publish_step.png", 4));
    }

    @Override
    protected void readData(XmlObjectConfigurationReader reader) {
        super.readData(reader);
        messageKind = PublishedMessageType.valueOf(reader.readString(MESSAGE_KIND_SETTING_NAME,
                DEFAULT_MESSAGE_TYPE.name()));
        message = reader.readString(MESSAGE_SETTING_NAME, "");
    }

    protected boolean sendMessage(Client client, Message<?> message, CancellationToken cancellationToken,
            WsdlTestStepResult testStepResult, long maxTime) {
        long timeout;
        if (maxTime == Long.MAX_VALUE) {
            timeout = -1;
        } else {
            timeout = maxTime - System.nanoTime();
            if (timeout <= 0) {
                testStepResult.addMessage(TIMEOUT_EXPIRED_MSG);
                testStepResult.setStatus(TestStepResult.TestStepStatus.FAILED);
                return false;
            }
        }

        if (timeout <= -1) {
            client.sendMessage(message, timeout);
        } else {
            client.sendMessage(message, TimeUnit.NANOSECONDS.toMillis(timeout));
        }

        testStepResult.setSize(message.size());

        return waitInternal(client, cancellationToken, testStepResult, maxTime,
                "Unable send message to HNClient.");
    }

    @Override
    public void setIcon(ImageIcon newIcon) {
        if (iconAnimator != null && newIcon == iconAnimator.getBaseIcon()) {
            return;
        }
        super.setIcon(newIcon);
    }

    public void setMessage(String value) {
        setProperty("message", MESSAGE_PROP_NAME, value);
    }

    public void setResponse(String value) {
        setProperty("response", RECEIVED_MESSAGE_PROP_NAME, value);
        logger.info("setResponse {}", value);
    }
    
    public void assertResponse(SubmitContext context) {
        
        for (WsdlMessageAssertion assertion : assertionsSupport.getAssertionList()) {
            applyAssertion(assertion, context);
        }
        getNotifier().notifyChange();
     }
    
    public Environment getActiveEnvironment() {
    	return getTestCase().getProject().getActiveEnvironment();
    }
    
    public static String getHNSClientEndpoint(Environment environment) {
    	if (environment == null) {
    		return ENDPOINT_UNDEFINED;
    	}
    	Service service = environment.getService(API_NAME, ServiceConfig.Type.REST);
    	Endpoint endpoint = service.getEndpoint();
    	logger.info("Endpoint: " + endpoint.getEndpointString());

    	return endpoint.getEndpointString();
    }
    
    public void setMessageKind(PublishedMessageType newValue) {
        if (messageKind == newValue) {
            return;
        }
        PublishedMessageType old = messageKind;
        messageKind = newValue;
        updateData();
        notifyPropertyChanged("messageKind", old, newValue);
        firePropertyValueChanged(MESSAGE_TYPE_PROP_NAME, old.toString(), newValue.toString());
    }

    @Override
    protected void updateState() {
        final AssertionStatus oldAssertionStatus = assertionStatus;
        if (getResponse() != null) {
            int cnt = getAssertionCount();
            if (cnt == 0) {
                assertionStatus = AssertionStatus.UNKNOWN;
            } else {
                assertionStatus = AssertionStatus.VALID;
                for (int c = 0; c < cnt; c++) {
                    if (getAssertionAt(c).getStatus() == AssertionStatus.FAILED) {
                        assertionStatus = AssertionStatus.FAILED;
                        break;
                    }
                }
            }
        } else {
            assertionStatus = AssertionStatus.UNKNOWN;
        }
        if (oldAssertionStatus != assertionStatus) {
            final AssertionStatus newAssertionStatus = assertionStatus;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    notifyPropertyChanged("assertionStatus", oldAssertionStatus, newAssertionStatus);
                }
            });
        }
        if (iconAnimator == null) {
            return;
        }
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if ((testMonitor != null)
                && (testMonitor.hasRunningLoadTest(getTestStep().getTestCase()) || testMonitor
                .hasRunningSecurityTest(getTestStep().getTestCase()))) {
            setIcon(disabledStepIcon);
        } else {
            ImageIcon icon = iconAnimator.getIcon();
            if (icon == iconAnimator.getBaseIcon()) {
                setIcon(getIcon());
            }
        }
    }
    
    private class PropertyChangeNotifier {
        private Assertable.AssertionStatus oldStatus;
        
        private ImageIcon oldIcon;
        
        public PropertyChangeNotifier() {
          updateProperties();
        }
        
        public void notifyChange() {
        logger.info("notifyChange");
          Assertable.AssertionStatus newStatus = getAssertionStatus();
          ImageIcon newIcon = getIcon();
          if (this.oldStatus != newStatus)
            notifyPropertyChanged(RestTestRequest.STATUS_PROPERTY, this.oldStatus, newStatus); 
          if (this.oldIcon != newIcon)
            notifyPropertyChanged(RestTestRequest.ICON_PROPERTY, this.oldIcon, getIcon()); 
          this.oldStatus = newStatus;
          this.oldIcon = newIcon;
        }
        
        public void updateProperties() {
          this.oldStatus = getAssertionStatus();
          this.oldIcon = getIcon();
        }
      }
    
    public static class PublishIconAnimator<T extends PublishTestStep> extends IconAnimator<T> implements SubmitListener {
        public PublishIconAnimator(T modelItem, String baseIcon, String baseAnimateIcon, int iconCount) {
          super((T)modelItem, baseIcon, baseAnimateIcon, iconCount);
        }
        
        public boolean beforeSubmit(Submit submit, SubmitContext context) {
          if (isEnabled() && submit.getRequest() == getTarget())
            start(); 
          return true;
        }
        
        public void afterSubmit(Submit submit, SubmitContext context) {
          if (submit.getRequest() == getTarget())
            stop(); 
        }
      }
    
    public PublishIconAnimator<?> getIconAnimator() {
        return this.iconAnimator;
      }
      
      public void setIconAnimator(PublishIconAnimator<?> iconAnimator) {
        if (this.iconAnimator != null)
          removeSubmitListener(this.iconAnimator); 
        this.iconAnimator = iconAnimator;
        if (RuntimeEnvironment.usingGraphicalEnvironment())
          addSubmitListener(this.iconAnimator); 
      }

      
      public void addSubmitListener(SubmitListener listener) {
    	    this.submitListeners.add(listener);
    	  }
    	  
    	  public void removeSubmitListener(SubmitListener listener) {
    	    this.submitListeners.remove(listener);
    	  }
    
    @Override
//    public ImageIcon getIcon() {
//        switch (assertionStatus) {
//            case VALID: {
//            	logger.info("Getting valid icon");
//                //return UISupport.createCurrentModeIcon("ca/bc/gov/hlth/hl7v2plugin/valid_publish_step.png");
//            	return UISupport.createImageIcon("ca/bc/gov/hlth/hl7v2plugin/valid_hl7_request_step.png");
//            }
//            case FAILED: {
//            	logger.info("Getting failed icon");
//                //return UISupport.createCurrentModeIcon("ca/bc/gov/hlth/hl7v2plugin/invalid_publish_step.png");
//                return UISupport.createImageIcon("ca/bc/gov/hlth/hl7v2plugin/invalid_hl7_request_step.png");
//            }
//            default: {
//            	return iconAnimator;
//                return unknownStepIcon;
//            }
//        }
//    }
    
    public ImageIcon getIcon() {
        if (isForLoadTest() || getIconAnimator() == null) {
          return null; 
        }
        TestMonitor testMonitor = ApplicationEnvironment.getTestMonitor();
        if (testMonitor != null && (
          testMonitor.hasRunningLoadTest((TestCase)getTestStep().getTestCase()) || testMonitor
          .hasRunningSecurityTest((TestCase)getTestStep().getTestCase())))
          return this.disabledStepIcon; 
        ImageIcon icon = getIconAnimator().getIcon();
        if (icon == getIconAnimator().getBaseIcon()) {
          Assertable.AssertionStatus status = getAssertionStatus();
          if (status == Assertable.AssertionStatus.VALID) {
              return UISupport.createCurrentModeIcon("ca/bc/gov/hlth/hl7v2plugin/valid_hl7_request_step.png");        	  
          } 
          if (status == Assertable.AssertionStatus.FAILED) {
        	  return UISupport.createCurrentModeIcon("ca/bc/gov/hlth/hl7v2plugin/invalid_hl7_request_step.png");
          }
          if (status == Assertable.AssertionStatus.UNKNOWN) {
        	  return this.unknownStepIcon;
          }
        } 
        return icon;
      }

    @Override
    protected void writeData(XmlObjectBuilder builder) {
        super.writeData(builder);
        if (messageKind != null) {
            builder.add(MESSAGE_KIND_SETTING_NAME, messageKind.name());
        }
        builder.add(MESSAGE_SETTING_NAME, message);
        for (TestAssertionConfig assertionConfig : assertionConfigs) {
            builder.addSection(ASSERTION_SECTION, assertionConfig);
        }
    }

    private void initAssertions(TestStepConfig testStepData) {
        if (testStepData != null && testStepData.getConfig() != null) {
            XmlObject config = testStepData.getConfig();
            XmlObject[] assertionsSections = config.selectPath("$this/" + ASSERTION_SECTION);
            for (XmlObject assertionSection : assertionsSections) {
                TestAssertionConfig assertionConfig;
                try {
                    assertionConfig = TestAssertionConfig.Factory.parse(assertionSection.toString());
                } catch (XmlException e) {
                    logger.error(e.getMessage(), e);
                    continue;
                }
                assertionConfigs.add(assertionConfig);
            }
        }
        assertionsSupport = new AssertionsSupport(this, new AssertableConfigImpl());
    }

    @Override
    public TestAssertion cloneAssertion(TestAssertion source, String name) {
        return assertionsSupport.cloneAssertion(source, name);
    }

	@Override
	public TestAssertion addAssertion(String selection) {
		logger.info("Adding assertion");
		getNotifier().updateProperties();
		try {
			WsdlMessageAssertion assertion = assertionsSupport.addWsdlAssertion(selection);
			if (assertion == null) {
				return null;
			}

			if (response != null) {
				applyAssertion(assertion, new WsdlSubmitContext(this));
				updateState();
				getNotifier().notifyChange();
			}

			return assertion;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public void removeAssertion(TestAssertion assertion) {
		logger.info("removeAssertion");
		getNotifier().updateProperties();
		try {
			assertionsSupport.removeAssertion((WsdlMessageAssertion) assertion);

		} finally {
			((WsdlMessageAssertion) assertion).release();
			getNotifier().notifyChange();
		}
		updateState();
	}
	
    @Override
    public TestAssertion moveAssertion(int ix, int offset) {
    	logger.debug("moveAssertion");
    	getNotifier().updateProperties();
        WsdlMessageAssertion assertion = assertionsSupport.getAssertionAt(ix);
        try {
            return assertionsSupport.moveAssertion(ix, offset);
        } finally {
            assertion.release();
            updateState();
            getNotifier().notifyChange();
        }
    }
    
	@Override
	public TestAssertion insertAssertion(TestAssertion assertion, int index) {
		logger.debug("insertAssertion");
		getNotifier().updateProperties();
		try {
			return assertionsSupport.insertAssertion(assertion, index);
		} finally {
			getNotifier().notifyChange();
		}
	}

    @Override
    public void addAssertionsListener(AssertionsListener listener) {
        assertionsSupport.addAssertionsListener(listener);
    }

    private void applyAssertion(WsdlMessageAssertion assertion, SubmitContext context) {
    	logger.info("applyAssertion");
        assertion.assertProperty(this, RECEIVED_MESSAGE_PROP_NAME, messageExchange, context);
        logger.info("assertion status {}", assertion.getStatus());
    }

    private void assertReceivedMessage() {
        if (getResponse() != null) {
            for (WsdlMessageAssertion assertion : assertionsSupport.getAssertionList()) {
                applyAssertion(assertion, new WsdlSubmitContext(this));
            }
        }
        updateState();
    }



    @Override
    public String getAssertableContent() {
        return getResponse();
    }

    @Override
    public String getAssertableContentAsXml() {
        return getResponse();
    }

    @Override
    public TestAssertionRegistry.AssertableType getAssertableType() {
        return TestAssertionRegistry.AssertableType.BOTH;
    }

    @Override
    public TestAssertion getAssertionAt(int c) {
        return assertionsSupport.getAssertionAt(c);
    }

    @Override
    public TestAssertion getAssertionByName(String name) {
        return assertionsSupport.getAssertionByName(name);
    }

    @Override
    public int getAssertionCount() {
        return assertionsSupport.getAssertionCount();
    }

    @Override
    public List<TestAssertion> getAssertionList() {
        return new ArrayList<TestAssertion>(assertionsSupport.getAssertionList());
    }

    @Override
    public Map<String, TestAssertion> getAssertions() {
        return assertionsSupport.getAssertions();
    }

    @Override
    public AssertionStatus getAssertionStatus() {
        return assertionStatus;
    }

    @Override
    public String getDefaultAssertableContent() {
        return "";
    }

    public MessageType getExpectedMessageType() {
        return expectedMessageType;
    }

    @Override
    public Interface getInterface() {
        return null;
    }

    @Override
    public TestStep getTestStep() {
        return this;
    }


    @Override
    public void removeAssertionsListener(AssertionsListener listener) {
        assertionsSupport.removeAssertionsListener(listener);
    }

    public void setExpectedMessageType(PublishTestStep.MessageType value) {
        setProperty("expectedMessageType", null, value);
    }

    private class MessageExchangeImpl extends AbstractNonHttpMessageExchange<PublishTestStep> {

        public MessageExchangeImpl(PublishTestStep modelItem) {
            super(modelItem);
        }

        @Override
        public String getEndpoint() {
            return null;
        }

        @Override
        public boolean hasRequest(boolean ignoreEmpty) {
        	logger.info("hasREsponse");
            return false;
        }

        @Override
        public boolean hasResponse() {
        	logger.info("hasREsponse");
            return false;
        }

        @Override
        public Response getResponse() {
        	logger.info("getResponse");
            return null;
        }

        @Override
        public String getRequestContent() {
        	logger.info("getRequestContent");
        	return getModelItem().getProperty(RECEIVED_MESSAGE_PROP_NAME).getValue();
            //return null;
        }

        @Override
        public String getResponseContent() {
        	logger.info("getResponseContent");
            return null;
        }

        @Override
        public long getTimeTaken() {
            return 0;
        }

        @Override
        public long getTimestamp() {
            return System.currentTimeMillis();
        }

        @Override
        public boolean isDiscarded() {
            return false;
        }

    }

    protected enum MessageType implements ConnectedTestStepPanel.UIOption {

        Text("Text (UTF-8)"), BinaryData("Raw binary data"), IntegerNumber("Integer number"), FloatNumber(
                "Float number");
        private String title;

        MessageType(String title) {
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }

    private class AssertableConfigImpl implements AssertableConfig {

        @Override
        public TestAssertionConfig addNewAssertion() {
        	logger.info("Add new assertion");
            TestAssertionConfig newConfig = TestAssertionConfig.Factory.newInstance();
            assertionConfigs.add(newConfig);
            return newConfig;
        }

        @Override
        public List<TestAssertionConfig> getAssertionList() {
            return assertionConfigs;
        }

        @Override
        public TestAssertionConfig insertAssertion(TestAssertionConfig source, int ix) {
            TestAssertionConfig conf = TestAssertionConfig.Factory.newInstance();
            conf.set(source);
            assertionConfigs.add(ix, conf);
            updateData();
            return conf;
        }

        @Override
        public void removeAssertion(int ix) {
            assertionConfigs.remove(ix);
            updateData();
        }
    }


    @Override
    public void loadTestStarted(LoadTestRunner runner) {
  	  logger.debug("loadTestStarted");
  	  updateState();
    }

    
  @Override
  public void loadTestFinished(LoadTestRunner runner) {
	  logger.debug("loadTestFinished");
	  updateState();
  }


  @Override
  public void securityTestStarted(SecurityTestRunner runner) {
	  logger.debug("securityTestStarted");
	  updateState();    
  }

  @Override
  public void securityTestFinished(SecurityTestRunner runner) {
	  logger.debug("securityTestFinished");
	  updateState();    
  }

  @Override
  public void testCaseStarted(TestCaseRunner runner) {
	  logger.debug("testCaseStarted");
  }
  
  @Override
  public void testCaseFinished(TestCaseRunner runner) {
	  logger.info("testCaseFinished");
  }

  @Override
  public void mockServiceStarted(MockRunner runner) {
	  logger.info("mockServiceStarted");
  }

  @Override
  public void mockServiceStopped(MockRunner runner) {
	  logger.info("mockServiceStopped");
  }
  
  @Override
  public void projectStarted(ProjectRunner projectRunner) {
    logger.info("projectStarted");
  }

  @Override
  public void projectFinished(ProjectRunner projectRunner) {
    logger.info("projectFinished");
  }

  @Override
  public void testSuiteStarted(TestSuiteRunner testSuiteRunner) {
    logger.info("testSuiteStarted");
  }

  @Override
  public void testSuiteFinished(TestSuiteRunner testSuiteRunner) {
    logger.info("testSuiteFinished");
  }
}
