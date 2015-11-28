import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class ProtocolUtilities {

	public static final int KEY_SIZE_AES = 128;
	
	public static void printByteArray(String msg, byte[] byteArray) { //used for debugging
		System.out.println(msg);
		System.out.println("Total: " + byteArray.length + " bytes.");
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < byteArray.length; i++) {
		    result.append(String.format("%02x", byteArray[i]));
		    if ((i+1) % 16 == 0 )
		    	result.append("\n");
		    else if ((i+1) % 2 == 0 )
		    	result.append(" ");
		}
		System.out.println(result.toString());
	}

	public static ArrayList<String> consumeAndBreakHeader(InputStream in) throws IOException {
		ArrayList<Character> pipeline = new ArrayList<>();
		StringBuilder header = new StringBuilder();
		int c;
		while ((c = in.read()) != -1) {
			pipeline.add((char) c);
			header.append((char) c);
			if (pipeline.size() != 2) // pipeline not full
				continue;
			if (pipeline.get(0) == '\n' && pipeline.get(1) == '\n')
				break;
			pipeline.remove(0); // keep track of only the recent 2 bytes
		}
		if (header.length() == 0)
			return null;
		ArrayList<String> headerParts = new ArrayList<String>();
		Scanner scanner = new Scanner(header.toString());
		scanner.useDelimiter("\n");
		while (scanner.hasNext()) {
			headerParts.add(scanner.next());
		}
		scanner.close();
		return headerParts;
	}
}
