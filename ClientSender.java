import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Scanner;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class ClientSender {

	private static String hostName;
	private static int portNumber;
	
	private static void sendEcryptedAesKEY(OutputStream out, byte[] publicKey, byte[] aesKey)  {
		try {
			Cipher pkCipher = Cipher.getInstance("RSA");
			PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
			pkCipher.init(Cipher.ENCRYPT_MODE, pk);
			ByteArrayOutputStream tempByteStream = new ByteArrayOutputStream();
			CipherOutputStream cipherStream = new CipherOutputStream(tempByteStream, pkCipher);
			cipherStream.write(aesKey);
			cipherStream.close();
			tempByteStream.writeTo(out);
		} catch (IOException | InvalidKeySpecException
				| NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException e) {
			System.err.println("Failed to send encrypted AES key");
			System.exit(1);
		}
		
	}
	private static void sendFile(byte[] publicKey, byte[] aesKey,File file) {
		try (Socket socket = new Socket(hostName, portNumber);
				BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
				BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			) 
			{
			// send header and encrypted AES. AES is encrypted using private RSA key.
			out.write("FILE TRANSFER\n\n".getBytes("ASCII"));
			sendEcryptedAesKEY(out,publicKey,aesKey);
			// Encrypt the name of the file using AES and send it over the socket
			ByteArrayInputStream fileNameStream = new ByteArrayInputStream((file.getName()  + "\n").getBytes("ASCII"));
			ByteArrayOutputStream encryptedFileNameStream = new ByteArrayOutputStream();
			SecretKeySpec aeskeySpec = new SecretKeySpec(aesKey, "AES");
			Cipher aesCipher = Cipher.getInstance("AES");
			aesCipher.init(Cipher.ENCRYPT_MODE, aeskeySpec);
			CipherOutputStream cipherOutStream = new CipherOutputStream(encryptedFileNameStream, aesCipher);
			ProtocolUtilities.sendBytes(fileNameStream,cipherOutStream);
			cipherOutStream.close();
			encryptedFileNameStream.writeTo(out);
			// send the actual file
			FileInputStream fileStream = new FileInputStream(file);
			CipherOutputStream os = new CipherOutputStream(out, aesCipher);
			ProtocolUtilities.sendBytes(fileStream,os);
			os.close();
			
		} catch (IOException | GeneralSecurityException e) {
			System.err.println("Failed to send file.");
			System.exit(1);
		} 
	}
	
	private static byte[] getPublicKey() {
		byte[] publicKey = null;
		try (Socket socket = new Socket(hostName, portNumber);
				BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
				BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			) 
			{
			out.write("GET PUBLIC KEY\n\n".getBytes("ASCII"));
			out.flush();
			ArrayList<String> headerParts = ProtocolUtilities.consumeAndBreakHeader(in);
			if (!headerParts.get(0).equals("PUBLIC KEY")) {
				System.err.println("Failed to obtain public key. The Server responded with the following:");
				for (String msg: headerParts) System.err.println(msg);
				System.exit(1);
			}
			int keySize = Integer.parseInt(headerParts.get(1));
			publicKey = new byte[keySize];
			in.read(publicKey);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (NumberFormatException e) {
			System.err.println("Invalid key size");
			System.exit(1);
		}
		return publicKey;
	}
	
	private static byte[] generateAesKey(){
		byte[] secretAesKey = null;
		try {
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			kgen.init(ProtocolUtilities.KEY_SIZE_AES); // AES key length 128 bits (16 bytes)
			secretAesKey = kgen.generateKey().getEncoded();
		} catch(NoSuchAlgorithmException e) {
			System.err.println("Failed to generate AES key.");
			System.exit(1);
		}
		return secretAesKey;
	}

	public static void main(String[] args) throws IOException, GeneralSecurityException {
		hostName = "localhost";
		portNumber = 8080;
		byte[] publicRsaKey = getPublicKey();
		byte[] secretAesKey = generateAesKey();
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter the name of the file to send: ");
		String fileName = scanner.next();
		sendFile(publicRsaKey,secretAesKey,new File(fileName));
		scanner.close();
	}
}