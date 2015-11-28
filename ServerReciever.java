import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;

public class ServerReciever {
	private static final int PORT = 9001;

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running.");
		ServerSocket listener = new ServerSocket(PORT);
		try {
			while (true) {
				new Handler(listener.accept()).start();
			}
		} finally {
			listener.close();
		}
	}

	private static class Handler extends Thread {
		private Socket socket;
		private InputStream in;
		private OutputStream out;

		private void sendPublicKey() throws UnsupportedEncodingException,
				IOException, InterruptedException {
			StringBuilder messageHeader = new StringBuilder();
			messageHeader.append("PUBLIC KEY\n");
			File publicKeyFile = new File("public.der");
			long keyLength = publicKeyFile.length();
			messageHeader.append(keyLength + "\n\n");
			try {
				BufferedInputStream publicKeyStream;
				publicKeyStream = new BufferedInputStream(new FileInputStream(
						publicKeyFile));
				byte[] key = new byte[(int) keyLength];
				publicKeyStream.read(key);
				publicKeyStream.close();
				byte[] byteHeader = messageHeader.toString().getBytes("ASCII");
				out.write(byteHeader);
				out.write(key);
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
				sendErrorMessage("PUBLIC KEY FAIL");
				System.out.println("failed sending public key.");
			}
		}

		private void sendErrorMessage(String msg)
				throws UnsupportedEncodingException, IOException {
			msg = "ERROR\n" + msg + "\n\n";
			out.write(msg.getBytes("ASCII"));
		}

		private void readAndDecryptAesKey(byte[] privateKey)
				throws IOException, NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeySpecException,
				InvalidKeyException {
			// read the encrypted AES key from the socket
			byte[] encryptedAesKey = new byte[ProtocolUtilities.KEY_SIZE_AES * 2];
			in.read(encryptedAesKey);
			ProtocolUtilities.printByteArray("Encrypted key:", encryptedAesKey);
			// put the private RSA key in the appropriate data structure
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKey);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			PrivateKey pk = kf.generatePrivate(privateKeySpec);
			// decrypt the AES key using the private RSA key
			Cipher pkCipher = Cipher.getInstance("RSA");
			pkCipher.init(Cipher.DECRYPT_MODE, pk);
			CipherInputStream is = new CipherInputStream(new ByteArrayInputStream(encryptedAesKey), pkCipher);
			byte[] aesKey = new byte[ProtocolUtilities.KEY_SIZE_AES / 8];
			is.read(aesKey);
			ProtocolUtilities.printByteArray("Decrypted AES key: ",aesKey);
			is.close();
		}

		public Handler(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				in = new BufferedInputStream(socket.getInputStream());
				out = new BufferedOutputStream(socket.getOutputStream());
				ArrayList<String> headerParts = ProtocolUtilities
						.consumeAndBreakHeader(in);
				String command = headerParts.get(0);
				switch (command) {
				case "GET PUBLIC KEY":
					sendPublicKey();
					System.out.println("Sent public key!");
					break;
				case "RECIEVE FILE":
					byte[] privateRSAkey = Files.readAllBytes(new File("private.der").toPath());
					readAndDecryptAesKey(privateRSAkey);
				default:
					// sendErrorMessage("INVALID COMMAND");
				}
			} catch (IOException | InterruptedException e) {
				System.out.println(e);
				System.out.println("Server Error!!!");
			} catch (InvalidKeyException | NoSuchAlgorithmException
					| NoSuchPaddingException | InvalidKeySpecException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
