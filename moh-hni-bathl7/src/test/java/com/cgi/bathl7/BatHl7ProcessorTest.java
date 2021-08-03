package com.cgi.bathl7;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.cgi.bathl7.BatHL7Processor;

public class BatHl7ProcessorTest {

	@Test
	public void testReadFile() {
		BatHL7Processor bat = new BatHL7Processor();
		List<String> readFile = null;
		try {
			readFile = bat.readFile("src/test/resources/sample/", "R031.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertEquals(4, readFile.size());
	}

	@Test
	public void testExpectedException() {
		BatHL7Processor bat = new BatHL7Processor();

		Assertions.assertThrows(IOException.class, () -> {
			bat.readFile("src/test/resources/sample/", "R032.txt");
		});
	}

	@Test
	public void testEmptyFile() {
		BatHL7Processor bat = new BatHL7Processor();
		List<String> readFile = null;
		try {
			readFile = bat.readFile("src/test/resources/sample/", "EmptyFile.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertEquals(readFile.size(), 0);

	}

	@Test
	public void testWriteFile() {

		List<String> responseList = new ArrayList<String>();
		BatHL7Processor bat = new BatHL7Processor();
		List<String> readFile = null;
		responseList.add("MSH|^~\\&||BC01000165|RAIGT-PRSN-DMGR|BC00002014|1|xx|||D|2.4");
		responseList.add("MSH|^~\\&||BC01000166|RAIGT-PRSN-DMGR|BC00002014|2|xx|||D|2.4");
		responseList.add("MSH|^~\\&||BC01000167|RAIGT-PRSN-DMGR|BC00002014|3|xx|||D|2.4");

		try {
			BatHL7Processor.deleteFileIfExists("Test_Output.txt");
			bat.writeFile("Test_Output.txt", responseList);
			readFile = bat.readFile("C:/HNClient/", "Test_Output.txt");
			assertEquals(4, readFile.size());

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
