package ca.bc.gov.hlth.hnclientv2;

import java.util.UUID;

import org.apache.camel.spi.UuidGenerator;

/**
 * This implementation uses a Java.util.UUID to generate a unique Id
 * for logging and tracing
 */
public class TransactionIdGenerator implements UuidGenerator {
	
	
	@Override
	public String generateUuid() {
	    UUID uuid = UUID.randomUUID();		
        return uuid.toString();
	}

}
