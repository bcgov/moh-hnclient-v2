package com.cgi.hl7v2sendmessage;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.Base64;

public class HL7v2SendMessage {

    private static String v2Msg;

    static {
        v2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
                + "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n"
                + "PID||1234567890^^^BC^PH";
        byte[] encodedBytes = Base64.getEncoder().encode(v2Msg.getBytes());
        v2Msg = new String(encodedBytes);
    }

    private static String msg = "{\n" +
            "\t\"resourceType\": \"Bundle\",\n" +
            "\t\"id\": \"10bb101f-a121-4264-a920-67be9cb82c74\",\n" +
            "\t\"type\": \"message\",\n" +
            "\t\"timestamp\": \"2015-07-14T11:15:33+10:00\",\n" +
            "\t\"entry\": [\n" +
            "\t\t{\n" +
            "\t\t\t\"fullUrl\": \"urn:uuid:267b18ce-3d37-4581-9baa-6fada338038b\",\n" +
            "\t\t\t\"resource\": {\n" +
            "\t\t\t\t\"resourceType\": \"MessageHeader\",\n" +
            "\t\t\t\t\"id\": \"267b18ce-3d37-4581-9baa-6fada338038b\",\n" +
            "\t\t\t\t\"eventCoding\": {\n" +
            "\t\t\t\t\t\"system\": \"hl7-v2 message wrapper\",\n" +
            "\t\t\t\t\t\"code\": \"R03\"\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t\"source\": {\n" +
            "\t\t\t\t\t\"endpoint\": \"HNClient\"\n" +
            "\t\t\t\t}\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t{\n" +
            "\t\t\t\"fullUrl\": \"urn:uuid:3f34f-df333-cddddd3p3-fr45\",\n" +
            "\t\t\t\"resource\": {\n" +
            "\t\t\t\t\"resourceType\": \"Binary\",\n" +
            "\t\t\t\t\"id\": \"3f34f-df333-cddddd3p3-fr45\",\n" +
            "\t\t\t\t\"contentType\": \" x-application/hl7-v2+er7\",\n" +
            "\t\t\t\t\"data\": \"" +
            v2Msg +
            "\"\n" +
            "\t\t\t}\n" +
            "\t\t}\n" +
            "\t]\n" +
            "}";

    public static void main(String[] args) throws Exception {
        try (Socket sock = new Socket("127.0.0.1", 8080)) {
            String output;
            //String msg = readInput(args[0]);
            //String msg = "This is a test message from a mock Point of Service";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            /*
            The header is a vertical tab character <VT> its hex value is 0x0b.
            The trailer is a file separator character <FS> (hex 0x1c) immediately
            followed by a carriage return <CR> (hex 0x0d)
            http://healthstandards.com/blog/2007/05/02/hl7-mlp-minimum-layer-protocol-defined/
             */
            //baos.write(0x0b); //header byte
            //baos.write(msg.length()); //length... does this need to be padded?
            baos.write(v2Msg.getBytes());
//            baos.write(0x1c); //trailing bytes
//            baos.write(0x0d);
            byte[] message = baos.toByteArray();

            sock.getOutputStream().write(message);
            sock.getOutputStream().flush();
            byte[] response = readStream(sock.getInputStream());
            output = new String(response, "UTF-8");
            Scanner s = new Scanner(output);
            while (s.hasNextLine()) {
                System.out.println(s.nextLine());
            }
        }
    }

    private static String readInput(String filename) throws Exception {
        FileInputStream fis = new FileInputStream(filename);
        return new String(readStream(fis));
    }

    private static byte[] readStream(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[1024];
        int len = is.read(buff);
        while (len > 0) {
            baos.write(buff, 0, len);
            if (len == buff.length) {
                len = is.read(buff);
            } else {
                len = 0;
            }
        }
        return baos.toByteArray();
    }

}
