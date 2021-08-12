package com.cgi.bathl7;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BatHl7ProcessorTest {

	@Test
	public void testReadFile() throws IOException {
		BatHL7Processor bat = new BatHL7Processor();
		List<String> readFile;

		readFile = bat.readFile("src/test/resources/sample/", "R031.txt");

		assertEquals(4, readFile.size());
	}

	@Test
	public void testExpectedException() {
		BatHL7Processor bat = new BatHL7Processor();

		Assertions.assertThrows(IOException.class, () -> bat.readFile("src/test/resources/sample/", "R032.txt"));
	}

	@Test
	public void testEmptyFile() throws IOException {
		BatHL7Processor bat = new BatHL7Processor();
		List<String> readFile;

		readFile = bat.readFile("src/test/resources/sample/", "EmptyFile.txt");

		assertEquals(0, readFile.size());
	}

	@Test
	public void testWriteFile() throws IOException {

		List<String> responseList = new ArrayList<>();
		BatHL7Processor bat = new BatHL7Processor();
		List<String> readFile;
		responseList.add("MSH|^~\\&||BC01000165|RAIGT-PRSN-DMGR|BC00002014|1|xx|||D|2.4");
		responseList.add("MSH|^~\\&||BC01000166|RAIGT-PRSN-DMGR|BC00002014|2|xx|||D|2.4");
		responseList.add("MSH|^~\\&||BC01000167|RAIGT-PRSN-DMGR|BC00002014|3|xx|||D|2.4");

		BatHL7Processor.deleteFileIfExists("Test_Output.txt");
		bat.writeFile("Test_Output.txt", responseList);
		readFile = bat.readFile("C:/HNClient/", "Test_Output.txt");
		assertEquals(4, readFile.size());

	}
}
