package ca.bc.gov.hlth.hnclientv2.keystore;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.camel.util.FileUtil;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenewKeys {

    private static final Logger logger = LoggerFactory.getLogger(RenewKeys.class);

    private static final String BACKUP_DIRECTORY = "backup";
    
    private static final String BACKUP_TIMESTAMP_FORMAT = "yyyyMMddhhmmss";
    
    public static void renewKeys(String accessToken, int certExpiryYears,
                                 String clientId, String pathToKeystore, String pathToCert,
                                 String certUploadEndpoint,
                                 String keystorePassword) throws Exception {

        //Generate a keypair
        KeyPair kp = KeystoreTools.generateKeyPair();

        //Generate an x509 certificate using the keypair (required for creating java key stores)
        X509Certificate x509Certificate = KeystoreTools.generateX509(kp, certExpiryYears, clientId);
        X509Certificate[] certChain = new X509Certificate[1];
        certChain[0] = x509Certificate;

        //Generate a java key store
        KeyStore ks = KeyStore.getInstance("jks");
        char[] password = keystorePassword.toCharArray();
        ks.load(null, password);
        ks.setKeyEntry(clientId, kp.getPrivate(), password, certChain);

        StringWriter sw = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(x509Certificate);
        }
        // Store the public key in cert format
        FileOutputStream certFos = new FileOutputStream(pathToCert, false);
        certFos.write(sw.toString().getBytes(StandardCharsets.UTF_8));
        certFos.close();

        // The Java 11 HttpClient doesn't support MultiPartBody so we needed a custom class
        // use the access token upload the public key to Keycloak
        MultiPartBodyPublisher publisher = new MultiPartBodyPublisher()
                .addPart("keystoreFormat", "Certificate PEM")
                .addPart("keyAlias", "undefined")
                .addPart("keyPassword", "undefined")
                .addPart("storePassword", "undefined")
                .addPart("file", Path.of(pathToCert));

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest certUpdateRequest = HttpRequest.newBuilder()
                .uri(URI.create(certUploadEndpoint))
                .header("Authorization",  accessToken)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Accept-Language", "en-CA,en-GB;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Content-Type", "multipart/form-data; boundary=" + publisher.getBoundary())
                .header("cache-control", "no-cache")
                .POST(publisher.build())
                .build();

        HttpResponse<String> response = httpClient.send(certUpdateRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) { //Check for a 200 in the update, we dont' want to save the new cert if it didn't upload
            throw new Exception("Error updating the certificate");
        }

        // Keycloak seems to take about an hour before the new cert starts working, a PUT to the client url seems to update it immediately
        // but this appears to wipe out the service account role required to update the client again in the future
        // for now manually toggling the auth method from clientId+Secret and back to signed JWT forces the new cert to take
        // This will not be an issue in a system where Keycloak "client management" api service handles the actual interaction with keycloak

        // Store the keystore.
        logger.info("New public key is: " + response.body());

        // Backup the keystore
        backupKeyStore(pathToKeystore);        

        FileOutputStream fos = new FileOutputStream(pathToKeystore, false);
        ks.store(fos, password);
        fos.close();
    }
    
    /**
     * Backs up the current keystore in case there are any issues and it needs to be restored.
     *  
     * @param pathToKeyStore
     */
    public static Path backupKeyStore(String pathToKeyStore) {
    	
    	String keyStoreDirectory = FileUtil.onlyPath(pathToKeyStore);
    	String fileName = FileUtil.stripExt(FileUtil.stripPath(pathToKeyStore));
    	String fileExt = FileUtil.onlyExt(pathToKeyStore);
    	String timestamp = new SimpleDateFormat(BACKUP_TIMESTAMP_FORMAT).format(new Date());

    	Path existingKeyStore = null;
    	Path backupKeyStore = null;

    	try {
    	
	    	// Put the backups in a separate directory to avoid cluttering up the main directory
	    	Path backupDirectory = Path.of(keyStoreDirectory, BACKUP_DIRECTORY);
	
	    	// Create the directory if it doesn't exist
	    	if (!Files.exists(backupDirectory)) {
	    		Files.createDirectory(backupDirectory);
	    	}
	    	
	    	// Add a timestamp to the files so they are unique and easily identifiable
	    	String backupFileName = fileName + "-" + timestamp + "." + fileExt;
	
	    	existingKeyStore = Path.of(pathToKeyStore);
	    	backupKeyStore = Path.of(backupDirectory.toString(), backupFileName);

			Files.copy(existingKeyStore, backupKeyStore);
			logger.info("Keystore {} backed up to {}", existingKeyStore.toString(), backupKeyStore.toString());
		} catch (IOException e) {
			logger.error("Keystore {} could not be backed up to {}", existingKeyStore.toString(), backupKeyStore.toString());
		}
    	return backupKeyStore;
    }

}
