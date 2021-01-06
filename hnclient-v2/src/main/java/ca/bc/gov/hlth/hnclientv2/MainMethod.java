package ca.bc.gov.hlth.hnclientv2;

import org.apache.camel.main.Main;

/**
 * Main class that boots the Camel application.
 */
public class MainMethod {

    public static void main(String... args) throws Exception {
        Main main = new Main();
        main.configure().addRoutesBuilder(new Route());
        main.run(args);
    }

}

