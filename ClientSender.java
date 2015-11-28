import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientSender {
	
	private static String hostName;
	private static int portNumber;

	public static ArrayList<String> breakHeader(String header) {
		if(header.length() == 0)
			return null;
		ArrayList<String> headerParts = new ArrayList<String>();
		Scanner scanner = new Scanner(header);
		scanner.useDelimiter("\n");
		while(scanner.hasNext()) {
			headerParts.add(scanner.next());
		}
		scanner.close();
		return headerParts;
	}
	private static byte[] getPublicKey() {
		try (	Socket socket = new Socket(hostName, portNumber);
				BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
				BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			) {
			out.write("GET PUBLIC KEY\n\n".getBytes("ASCII"));
			out.flush();
			ArrayList<String> headerParts = ProtocolUtilities.consumeAndBreakHeader(in);
			if (headerParts.size() < 2 && headerParts.get(0) != "PUBLIC KEY") {
				System.out.println("Invalid header for public key message");
				return null;
			}
			int keySize = Integer.parseInt(headerParts.get(1));
			byte[] publicKey = new byte[keySize];
			in.read(publicKey);
			return publicKey;
		} catch (UnknownHostException e) {
			System.err.println("Unknown host " + hostName);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Unable to get I/O for the connection "
					+ hostName);
			System.exit(1);
		} catch(NumberFormatException e) {
			System.err.println("Invalid key size");
		}
		return null;
	}

	public static void main(String[] args) throws IOException {
		hostName = "localhost";
		portNumber = 9001;
		byte[] publicKey = getPublicKey();
		System.out.println("Public key:");
		for(byte b : publicKey) {
			System.out.print(b+",");
		}

	}
}
