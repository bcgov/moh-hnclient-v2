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
 * This program is a command line application that takes as input a file
 * containing one or more HL7 transactions and transmits them to one or more
 * healthnetBC servers. The purpose of this application is two-fold: to exercise
 * the Client libraries with respect to healthnetBC HL7 messages; and to
 * exercise server applications in how they handle HL7 messages sent to them.
 * 
 * @author anumeha.srivastava
 *
 */
public class BatHL7Processor {

	private static final String LINE_SEPARATOR = "---------------------------------------";
	private static final Logger logger = LoggerFactory.getLogger(BatHL7Processor.class);

	/**
	 * This is the entry point of bathl7 tool
	 * 
	 * @param args arg[0] inputFile args[1] the outputFile args[2] the port
	 */
	public static void main(String[] args) {

		String address = null;
		String port = null;
		List<String> hl7TransactionResponse;
		try {
			if (args.length < 2 || args.length > 4) {
				logInfoMessage();
				return;
			}

			if (args.length > 2 && StringUtils.isNotBlank(args[2])) {
				address = args[2];
			}

			if (args.length > 3 && StringUtils.isNotBlank(args[3])) {
				port = args[3];
			}

			deleteFileIfExists(args[1]);

			BatHL7Processor batObj = new BatHL7Processor();

			List<String> lst = batObj.readFile(args[0]);
			hl7TransactionResponse = batObj.performTransaction(lst, address, port);

			batObj.writeFile(args[1], hl7TransactionResponse);

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * @param v2Messages - the v2 messages to send
	 * @param clientAddress - the HNClient address
	 * @param port - the HNClient listening port
	 */
	private List<String> performTransaction(List<String> v2Messages, String clientAddress, String port) {
		ConnectionHandler handler = new ConnectionHandler();
		List<String> hl7TransactionResponse = new ArrayList<>();
		AtomicInteger count = new AtomicInteger(0);

		v2Messages.forEach(v2Msg -> {
			logger.info("Sending Message : {}", count.incrementAndGet());
			String hl7Response;
			try {
				hl7Response = handler.socketConnection(v2Msg, clientAddress, port);
				hl7TransactionResponse.add(formatResponse(v2Msg, hl7Response));
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		});

		logger.info("End of Messages");
		return hl7TransactionResponse;

	}

	/**
	 * Decorates the output file
	 * 
	 * @param request - the message that was sent
	 * @param response - the response message
	 * @return the formatted response message
	 */
	private String formatResponse(String request, String response) {
		return String.format("%s%n%n%s%s%n", request, response, LINE_SEPARATOR);
	}

	/**
	 * The HL7 messages transmitted to the server come in the form of a text file,
	 * one segment per line. Each line in the text file holds a single segment of
	 * the HL7 message and is formatted exactly how it will be sent to the server.
	 * No reformatting of messages is performed by the application if header is
	 * missing, data segment becomes a part of the previous message. Users of this
	 * application may add, change or remove HL7 messages in the transaction file as
	 * they see fit.
	 * 
	 * @param fileLocation the location of input file
	 * @param file         the input file name
	 * @return list of formatted message.
	 * @throws IOException - thrown if the file can't be found
	 */
	public List<String> readFile(String file) throws IOException {

		String fileName = file;
		StringBuilder sb = null;

		List<String> hl7Transaction;
		try (Scanner scanner = new Scanner(new File(fileName))) {
			hl7Transaction = new ArrayList<>();
			while (scanner.hasNext()) {
				String nextLine = scanner.nextLine();

				if (nextLine.startsWith(HL7XferTransaction.HEADER_INDICATOR)) {
					if (sb != null) {
						hl7Transaction.add(sb.toString());
					}
					sb = new StringBuilder();
				}
				if (sb == null) {
					sb = new StringBuilder();
				}
				sb.append(nextLine).append("\r\n");
			}

		} catch (IOException fe) {
			logger.info("Unable to find file with the name {}", file);
			throw fe;
		}
		if (sb != null)
			hl7Transaction.add(sb.toString());

		return hl7Transaction;
	}

	/**
	 * Deletes the output file if already exists
	 * @throws IOException if an I/O error occurs
	 */
	public static void deleteFileIfExists(String fileName) throws IOException {
		Path path = Paths.get(fileName);
		Files.deleteIfExists(path);
	}

	/**
	 * @param responseFileName the output file name
	 * @param responseList the list of hl7 response received from server
	 */
	public void writeFile(String responseFileName, List<String> responseList) throws IOException {
		File file = new File(responseFileName);
		Date date = java.util.Calendar.getInstance().getTime();
		int i = 0;

		try (FileWriter fr = new FileWriter(file, true);
			BufferedWriter br = new BufferedWriter(fr);
			PrintWriter pr = new PrintWriter(br)
		) {
			pr.println("\nBathl7 transaction started: " + date + "\n\n");
			for (String hl7Response : responseList) {
				i++;
				pr.println("HL7 Transaction Log: Message " + i + "\n\n");
				pr.println(hl7Response);
			}
			pr.println("\nBathl7 transaction completed: " + date);
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * Generates a formatted message if command line arguments are missing
	 */
	protected static void logInfoMessage() {
		logger.info(String.format(
				"BATHL7 \nUsage: bathl7 input_file_name output_file_name client_address:port %n%s%n%s%n%s%n%s",
				"Where:", "input_file_name is the name of the input transaction file. Default is input.txt",
				"output_file_name is the name of the output response file.",
				"client_address:port is the ip/domain name, and port of the HNS-Client. Default is localhost:19430"));
	}

}
