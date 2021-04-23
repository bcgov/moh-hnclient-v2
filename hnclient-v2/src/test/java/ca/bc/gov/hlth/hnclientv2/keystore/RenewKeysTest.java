package ca.bc.gov.hlth.hnclientv2.keystore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

public class RenewKeysTest {

	@Test
	public void testBackupKeyStore() throws IOException {
		String fileText = "Hello World!"; 
		Path keyStore = Files.createTempFile("renew-keys-test", ".jks");
		Files.writeString(keyStore, fileText);

		Path backupKeyStore = RenewKeys.backupKeyStore(keyStore.toString());
		assertTrue(Files.exists(backupKeyStore));
		
		// Don't check the exact file name because it has a temporary identifier in it
		// and the timestamp may not be an exact match as it's in the past
		String backupKeyStorePath = backupKeyStore.toString();
		assertTrue(backupKeyStorePath.contains("backup"));
		assertTrue(backupKeyStorePath.contains("renew-keys-test"));
		assertTrue(backupKeyStorePath.contains(new SimpleDateFormat("yyyyMMddhhmmss").format(new Date())));
		assertTrue(backupKeyStorePath.endsWith(".jks"));

		// Verify the contents
		assertEquals(fileText, Files.readString(backupKeyStore));
	}
	
}
