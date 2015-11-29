import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

public class ClientSender {

	private static String hostName;
	private static int portNumber;
	
//	private static void sendEcryptedAesKEY(OutputStream out, byte[] publicKey, byte[] aesKey) 
//			throws IOException, GeneralSecurityException {
//		
//		ProtocolUtilities.printByteArray("Unencrypted AES:", aesKey);
//		Cipher pkCipher = Cipher.getInstance("RSA");
//		PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
//		pkCipher.init(Cipher.ENCRYPT_MODE, pk);
//		ByteArrayOutputStream tempByteStream = new ByteArrayOutputStream();
//		CipherOutputStream cipherStream = new CipherOutputStream(tempByteStream, pkCipher);
//		cipherStream.write(aesKey);
//		cipherStream.close();
//		tempByteStream.writeTo(out);
//	}
	private static void sendFile(byte[] publicKey, byte[] aesKey,File file) {
		try (Socket socket = new Socket(hostName, portNumber);
				BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
				BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			) 
			{
			// send header
			out.write("FILE TRANSFER\n\n".getBytes("ASCII"));
			// Encrypt the AES key using the private RSA key and send it over the socket
			ProtocolUtilities.printByteArray("Unencrypted AES:", aesKey);
			Cipher pkCipher = Cipher.getInstance("RSA");
			PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
			pkCipher.init(Cipher.ENCRYPT_MODE, pk);
			ByteArrayOutputStream tempByteStream = new ByteArrayOutputStream();
			CipherOutputStream cipherStream = new CipherOutputStream(tempByteStream, pkCipher);
			cipherStream.write(aesKey);
			cipherStream.close();
			tempByteStream.writeTo(out);
			// Encrypt the name of the file using AES and send it over the socket
//			ByteArrayInputStream fileNameStream = new ByteArrayInputStream(
//					(file.getName()  + "\n").getBytes("ASCII"));
//			ByteArrayOutputStream encryptedFileNameStream = new ByteArrayOutputStream();
			SecretKeySpec aeskeySpec = new SecretKeySpec(aesKey, "AES");
			Cipher aesCipher = Cipher.getInstance("AES");
			aesCipher.init(Cipher.ENCRYPT_MODE, aeskeySpec);
//			CipherOutputStream cipherOutStream = new CipherOutputStream(encryptedFileNameStream, aesCipher);
//			ProtocolUtilities.sendBytes(fileNameStream,cipherOutStream);
//			encryptedFileNameStream.writeTo(out);
//			out.flush();
			// send the actual file
			FileInputStream fileStream = new FileInputStream(file);
			CipherOutputStream os = new CipherOutputStream(out, aesCipher);
			ProtocolUtilities.sendBytes(fileStream,os);
			os.close();
			
			// ALTERNATIVE
			
			ByteArrayOutputStream temp = new ByteArrayOutputStream();
			FileInputStream fileStream1 = new FileInputStream(file);
			CipherOutputStream os1 = new CipherOutputStream(temp, aesCipher);
			ProtocolUtilities.sendBytes(fileStream1,os1);
			os1.close();
			byte[] encrypted = temp.toByteArray();
			ProtocolUtilities.printByteArray("encrypted file: ", encrypted);
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (NumberFormatException e) {
			System.err.println("Invalid key size");
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
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
			if (headerParts.size() < 2 && headerParts.get(0) != "PUBLIC KEY") {
				System.err.println("Invalid header for public key message");
				System.exit(1);;
			}
			int keySize = Integer.parseInt(headerParts.get(1));
			publicKey = new byte[keySize];
			in.read(publicKey);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
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
			System.err.println("Failed to generate AES key. Terminating...");
			System.exit(1);
		}
		return secretAesKey;
	}

	public static void main(String[] args) throws IOException, GeneralSecurityException {
		hostName = "localhost";
		portNumber = 9001;
		byte[] publicRsaKey = getPublicKey();
		//byte[] secretAesKey = generateAesKey();
		byte[] secretAesKey = new byte[]{0x34,0x7f,0x2b,0x4b,0x6c,0x7b,(byte) 0xf8,0x41,(byte) 0x99,(byte) 0xd3,0x26,(byte) 0xe3,(byte) 0xcb,(byte) 0xe0,(byte) 0x8e,(byte) 0xbd};
		sendFile(publicRsaKey,secretAesKey,new File("try.txt"));
	}
}