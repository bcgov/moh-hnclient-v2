package ca.bc.gov.hlth.hnclientv2;

import org.apache.camel.main.Main;

import ca.bc.gov.hlth.hnclientv2.handshake.server.HandshakeServer;

/**
 * Main class that boots the Camel application.
 */
public class MainMethod {

	public static void main(String... args) throws Exception {
		System.out.println("main method");

		HandshakeServer myServer = new HandshakeServer();

		Main main = new Main();
		main.configure().addRoutesBuilder(new Route());
		main.run(args);

	}

}
