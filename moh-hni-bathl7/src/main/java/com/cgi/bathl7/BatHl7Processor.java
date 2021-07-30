package com.cgi.bathl7;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This program is a command line application that takes as input a file containing one or more HL7 transactions 
 * and transmits them to one or more healthnetBC servers. The purpose of this application is two-fold: to exercise
 * the Client libraries with respect to healthnetBC HL7 messages; and to exercise server applications in how they 
 * handle HL7 messages sent to them.
 * @author anumeha.srivastava
 *
 */
public class BatHl7Processor {

	private StringBuilder sb;

	private String lineSeperater = "---------------------------------------";

	private List<String> hl7Transaction;

	private static final String fileLocation = "C://HNClient/";

	private static List<String> hl7TransactionResponse = new ArrayList<String>();

	private static final Logger logger = LoggerFactory.getLogger(BatHl7Processor.class);

	/**
	 * This is the entry point of bathl7 tool
	 * @param args
	 * arg[0] inputFile
	 * args[1] the outputFile
	 * args[2] the port
	 */
	public static void main(String[] args) {
		String address = null;
		String port = null;
		try {
			if (args.length < 2 || args.length > 4) {
				logInfoMessage();
				return;
			}

			if (args.length > 2 && StringUtils.isNotBlank(args[2]))
				address = args[2];

			if (args.length > 3 && StringUtils.isNotBlank(args[3]))
				port = args[3];				
			
			deleteFileIfExists(args[1]);

			performTransaction(args[0], address, port);

			writeFile(args[1], hl7TransactionResponse);

		} catch (NumberFormatException e) {
			logger.debug(e.getMessage());
		} catch (IOException e) {
			logger.debug(e.getMessage());
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}
	}

	
	/**
	 * @param inputFile
	 * @param clientAddress
	 * @param port
	 * @throws Exception
	 */
	private static void performTransaction(String inputFile, String clientAddress, String port) throws Exception {
		ConnectionHandler handler = new ConnectionHandler();
		BatHl7Processor bat = new BatHl7Processor();
		 AtomicInteger count=new AtomicInteger(0);
		List<String> lst = bat.readFile(fileLocation, inputFile);
		
		lst.forEach(v2Msg -> {			
			logger.info("Sending Message : {}", count.incrementAndGet());
			String hl7Response = null;
			try {
				hl7Response = handler.socketConnection(v2Msg, clientAddress, port);
			} catch (Exception e) {
				logger.debug(e.getMessage());
			}
			if (hl7Response != null) {
				hl7TransactionResponse.add(bat.formatResponse(v2Msg, hl7Response));
			}
		});
		
		logger.info("End of Messages");
	}

	/**
	 * Decorates the output file 
	 * @param request
	 * @param response
	 * @return
	 */
	private String formatResponse(String request, String response) {
		return String.format("%s%n%n%s%s%n", request, response, lineSeperater);
	}

	/**
	 * The HL7 messages transmitted to the server come in the form of a text file, one segment per line. 
	 * Each line in the text file holds a single segment of the HL7 message and is formatted exactly how 
	 * it will be sent to the server. No reformatting of messages is performed by the application  
	 * if header is missing, data segment becomes a part of the previous message.
	 * Users of this application may add, change or remove HL7 messages in the transaction file as they see fit.
	 * @param fileLocation the location of input file
	 * @param file the input file name
	 * @return list of formatted message.
	 * @throws IOException
	 */
	public List<String> readFile(String fileLocation, String file) throws IOException {

		String fileName = fileLocation + file;

		try (Scanner scanner = new Scanner(new File(fileName))) {
			hl7Transaction = new ArrayList<String>();
			while (scanner.hasNext()) {
				String nextLine = scanner.nextLine();

				if (nextLine.startsWith(Hl7xferTransaction.HEADER_INDICATOR)) {
					if (sb != null) {
						hl7Transaction.add(sb.toString());
					}
					sb = new StringBuilder();
				}
				if (sb == null) {
					sb = new StringBuilder();
				}
				sb.append(nextLine + "\n");
			}

		} catch (IOException fe) {
			logger.info("Unable to find file with the name {}", file);
			throw fe;
		}
		if(sb!=null) 
		hl7Transaction.add(sb.toString());
		
		return hl7Transaction;
	}

	/**
	 * Deletes the output file if already exists
	 * @return
	 * @throws IOException
	 */
	private static void deleteFileIfExists(String fileName) throws IOException {
		{
			Path path = Paths.get(fileLocation + fileName);
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {
				throw e;
			}
		}
	}

	/**
	 * @param responseFileName the output file name
	 * @param responseList the list of hl7 response received from server
	 */
	public static void writeFile(String responseFileName, List<String> responseList) throws IOException {
		File file = new File(fileLocation + responseFileName);
		FileWriter fr = null;
		BufferedWriter br = null;
		PrintWriter pr = null;
		Date date = java.util.Calendar.getInstance().getTime();
		int i = 0;
		try {
			fr = new FileWriter(file, true);
			br = new BufferedWriter(fr);
			pr = new PrintWriter(br);
			pr.println("\nBathl7 transactionn started:" + date+"\n\n");
			for (String hl7Response : responseList) {
				i++;
				pr.println("HL7 Transaction Log: Message " + i + "\n\n");
				pr.println(hl7Response);
			}
			pr.println("Bathl7 transaction completed" + date);
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				pr.close();
				br.close();
				fr.close();
			} catch (IOException e) {
				throw e;
			}
		}
	}

	/**
	 * Generates a formatted message if command line arguments are missing
	 */
	protected static void logInfoMessage() {
		logger.info(String.format(
				"%n BATHL7 \nUsage: bathl7 input_file_name output_file_name client_address:port %n%s%n%s%n%s%n%s",
				"Where:", "input_file_name is the name of the input transaction file. Default is input.txt",
				"output_file_name is the name of the output response file.",
				"client_address:port is the ip/domain name, and port of the HNS-Client. Default is localhost:19430"));
	}

}
