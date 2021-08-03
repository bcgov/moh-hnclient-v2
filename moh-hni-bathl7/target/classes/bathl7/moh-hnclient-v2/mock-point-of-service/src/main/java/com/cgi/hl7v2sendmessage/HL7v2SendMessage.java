package com.cgi.hl7v2sendmessage;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.Scanner;

import net.minidev.json.JSONObject;

import java.util.Base64;

public class HL7v2SendMessage {

    private static String v2Msg;

    static {
        /*v2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
                + "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n"
                + "PID||1234567890^^^BC^PH";*/
        
        v2Msg = "00000352MSH|^~\\&|HNWEB|BCAS_PCDES|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||^M\r\n" +
        	    "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4^M\r\n" +
        	    "PID||9879875914^^^BC^PH";
       // byte[] encodedBytes = Base64.getEncoder().encode(v2Msg.getBytes());
        //v2Msg = new String(encodedBytes);
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
        try (Socket sock = new Socket("142.34.205.122", 13627)) {
            String output;
           
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
           
           // String json= createJsonObject(v2Msg).toString();
            baos.write(v2Msg.getBytes());

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
    
	public static JSONObject createJsonObject(final String hl7Message) {
		
		JSONObject jsonObj = new JSONObject();		
		jsonObj.put("HL7_MESSAGE", hl7Message);
		
		return jsonObj;		
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
