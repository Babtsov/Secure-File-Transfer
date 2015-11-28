import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
	
	
	private static void sendEcryptedAesKEY(OutputStream out, byte[] publicKey, byte[] aesKey) 
			throws IOException, GeneralSecurityException {
		
		//new SecretKeySpec(aesKey, "AES");
		ProtocolUtilities.printByteArray("Unencrypted AES:", aesKey);
		Cipher pkCipher = Cipher.getInstance("RSA");
		PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
		pkCipher.init(Cipher.ENCRYPT_MODE, pk);
		ByteArrayOutputStream tempByteStream = new ByteArrayOutputStream();
		CipherOutputStream cipherStream = new CipherOutputStream(tempByteStream, pkCipher);
		cipherStream.write(aesKey);
		cipherStream.close();
		tempByteStream.writeTo(out);
	}
	private static void sendFile(byte[] publicKey, byte[] aesKey) {
		try (Socket socket = new Socket(hostName, portNumber);
				BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
				BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			) 
			{
			out.write("RECIEVE FILE\n\n".getBytes("ASCII"));
			sendEcryptedAesKEY(out,publicKey,aesKey);
			out.flush();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (NumberFormatException e) {
			System.err.println("Invalid key size");
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static byte[] getPublicKey() {
		try (Socket socket = new Socket(hostName, portNumber);
				BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
				BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			) 
			{
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
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (NumberFormatException e) {
			System.err.println("Invalid key size");
		}
		return null;
	}

	public static void main(String[] args) throws IOException, GeneralSecurityException {
		hostName = "localhost";
		portNumber = 9001;
		byte[] publicKey = getPublicKey();
		if (publicKey == null)
			System.exit(1);
		byte[] secretAesKey = null;
		try {
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			kgen.init(ProtocolUtilities.KEY_SIZE_AES); // AES key length 128 bits (16 bytes)
			secretAesKey = kgen.generateKey().getEncoded();
		} catch(NoSuchAlgorithmException e) {
			System.err.println("Failed to generate AES key. Terminating...");
			System.exit(1);
		}
		
		ProtocolUtilities.printByteArray("Public key:",publicKey);
		sendEcryptedAesKEY(new FileOutputStream(new File("EncryptedAES.der")),publicKey,secretAesKey);
		sendFile(publicKey,secretAesKey);
	}
}
