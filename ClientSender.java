import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientSender {
	private static String hostName;
	private static int portNumber;

	private static String sendMessage(String msg) {
		try (Socket echoSocket = new Socket(hostName, portNumber);
				PrintWriter out = new PrintWriter(echoSocket.getOutputStream(),
						true);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						echoSocket.getInputStream()));
			) {
			out.println(msg);
			String serverOutputLine;
			StringBuilder serverResponse = new StringBuilder();
			while ((serverOutputLine = in.readLine()) != null) {
				serverResponse.append(serverOutputLine);
				System.out.println(serverOutputLine);
			}
			return serverResponse.toString();
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + hostName);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to "
					+ hostName);
			System.exit(1);
		}
		throw new RuntimeException();
	}

	public static void main(String[] args) throws IOException {
//		if (args.length != 2) {
//			System.err
//					.println("Usage: java EchoClient <host name> <port number>");
//			System.exit(1);
//		}
//		hostName = args[0];
//		portNumber = Integer.parseInt(args[1]);
		hostName = "localhost";
		portNumber = 9001;
		String serverResponse = sendMessage("GET PUBLIC KEY");
//		ByteArrayInputStream responseStream = new ByteArrayInputStream(serverResponse.getBytes("UTF-8"));
//		Scanner scanner = new Scanner(responseStream);
//		String firstLine = scanner.nextLine();
//		if (!firstLine.equals("PUBLIC KEY")){
//			System.out.println("Error at recieving the public key from the server!");
//		}
//		int keylength = scanner.nextInt();
//		scanner.close();
//		byte[] publicKeyArray = new byte[keylength];
		
	}
}
