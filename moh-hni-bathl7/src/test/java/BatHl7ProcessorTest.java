import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.cgi.bathl7.BatHl7Processor;

class BatHl7ProcessorTest {

	@Test
	void testReadFile() {
		BatHl7Processor bat = new BatHl7Processor();
		List<String> readFile = null;
		try {
			readFile = bat.readFile("src/test/java/sample/", "R031.txt");
		} catch (IOException e) {			
			e.printStackTrace();
		}
		assertEquals(readFile.size(), 4);
	}

	@Test
	void testExpectedException() {
		BatHl7Processor bat = new BatHl7Processor();

		Assertions.assertThrows(IOException.class, () -> {
			List<String> readFile = bat.readFile("src/test/java/sample/", "R032.txt");
			assertEquals(readFile.size(), 4);
		});

	}
	
	@Test
	void testEmptyFile() {
		BatHl7Processor bat = new BatHl7Processor();
		List<String> readFile = null;
		try {
			readFile = bat.readFile("src/test/java/sample/", "EmptyFile.txt");
		} catch (IOException e) {			
			e.printStackTrace();
		}
		assertEquals(readFile.size(), 0);;

	}

}
