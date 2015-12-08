import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Scanner;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

public class ClientSender {

	private static String hostName;
	private static int portNumber;

	private static void sendEcryptedAesKEY(OutputStream out, byte[] publicKey,byte[] aesKey) 
			throws GeneralSecurityException, IOException {
		Cipher pkCipher = Cipher.getInstance("RSA");
		PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
		pkCipher.init(Cipher.ENCRYPT_MODE, pk);
		ByteArrayOutputStream tempByteStream = new ByteArrayOutputStream();
		CipherOutputStream cipherStream = new CipherOutputStream(tempByteStream, pkCipher);
		cipherStream.write(aesKey);
		cipherStream.close();
		tempByteStream.writeTo(out);
	}
	
	private static boolean sendFile(byte[] publicKey, byte[] aesKey, File file)
			throws FileNotFoundException, IOException, GeneralSecurityException {
		Socket socket = new Socket(hostName, portNumber);
		BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
		BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
		// send header and encrypted AES. AES is encrypted using private RSA key.
		out.write("FILE TRANSFER\n\n".getBytes("ASCII"));
		sendEcryptedAesKEY(out,publicKey,aesKey);
		// Encrypt the name of the file and its size using AES and send it over the socket
		String fileNameAndSize = new String(file.getName()  + "\n" + file.length() + "\n");
		ByteArrayInputStream fileInfoStream = new ByteArrayInputStream(fileNameAndSize.getBytes("ASCII"));
		SecretKeySpec aeskeySpec = new SecretKeySpec(aesKey, "AES");
		Cipher aesCipher = Cipher.getInstance("AES");
		aesCipher.init(Cipher.ENCRYPT_MODE, aeskeySpec);
		CipherOutputStream cipherOutStream = new CipherOutputStream(out, aesCipher);
		ProtocolUtilities.sendBytes(fileInfoStream,cipherOutStream);
		// send the the actual file itself and append some bytes so cipher would know it's the end of the file
		FileInputStream fileStream = new FileInputStream(file);
		ProtocolUtilities.sendBytes(fileStream,cipherOutStream);
		out.write(aesCipher.doFinal());
		out.write("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n".getBytes("ASCII")); 
		out.flush();
		ArrayList<String> serverResponse = ProtocolUtilities.consumeAndBreakHeader(in);
		socket.close();
		if (!serverResponse.get(0).equals("SUCCESS")) {
			System.err.println("Failed to send file. The Server responded with the following:");
			for (String msg : serverResponse)
				System.err.println(msg);
			return false;
		}
		return true;
	}
	
	private static byte[] getPublicKey() throws IOException {
		Socket socket = new Socket(hostName, portNumber);
		BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
		BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
		out.write("GET PUBLIC KEY\n\n".getBytes("ASCII"));
		out.flush();
		ArrayList<String> headerParts = ProtocolUtilities.consumeAndBreakHeader(in);
		if (!headerParts.get(0).equals("PUBLIC KEY")) {
			System.err.println("Failed to obtain public key. The Server responded with the following:");
			for (String msg : headerParts)
				System.err.println(msg);
			System.exit(1);
		}
		int keySize = Integer.parseInt(headerParts.get(1));
		byte[] publicKey = new byte[keySize];
		in.read(publicKey);
		socket.close();
		return publicKey;
	}

	private static byte[] generateAesKey() throws NoSuchAlgorithmException {
		byte[] secretAesKey = null;
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(ProtocolUtilities.KEY_SIZE_AES); // AES key length 128 bits (16 bytes)
		secretAesKey = kgen.generateKey().getEncoded();
		return secretAesKey;
	}

	public static void main(String[] args) {
		hostName = "localhost";
		portNumber = 8080;
		try {
			if (args.length == 2) {
				hostName = args[0];
				portNumber = Integer.parseInt(args[1]);
			} 
			else if (args.length == 0); // use defaults if no host name and port number are provided.
			else throw new IllegalArgumentException();
		} catch (IllegalArgumentException e) {
			System.out.println("Usage: java ClientSender [hostName portNumber]");
			System.exit(1);
		}
		System.out.println("Using host name: "+hostName + " and port number: " + portNumber + "...");
		byte[] publicRsaKey, secretAesKey;
		File[] currentDirFiles = new File(Paths.get(".").toAbsolutePath().toString()).listFiles();
		System.out.println("Available files in the current directory:");
		for (File f : currentDirFiles) {
			String fileName = f.getName();
			if (fileName.charAt(0) == '.') // ignore hidden files.
				continue;
			System.out.println(fileName);
		}
		try {
			publicRsaKey = getPublicKey();
			secretAesKey = generateAesKey();
			Scanner scanner = new Scanner(System.in);
			System.out.println("Enter the name of the file to send: ");
			String fileName = scanner.next();
			boolean isSuccessful = sendFile(publicRsaKey, secretAesKey, new File(fileName));
			if (isSuccessful){
				System.out.println("File was successfully sent.");
			} else {
				System.out.println("File was not sent.");
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			System.err.println("File not found.");
		} catch (IOException e) {
			System.err.println("There was an error connecting to the server.");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Failed to generate AES key.");
		} catch (GeneralSecurityException e) {
			System.err.println("Unknown security error.");
		}
	}
}