import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Scanner;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

public class ProtocolUtilities {

	public static final int KEY_SIZE_AES = 128;
	public static void printByteArray(String msg, byte[] byteArray) {
		System.out.println(msg);
		for (byte b : byteArray) {
			System.out.print(b + ",");
		}
		System.out.println();
	}

	public static void sendEcryptedAES(OutputStream out, byte[] publicKey) throws IOException, GeneralSecurityException {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(128); // AES key length 128 bits (16 bytes)
		byte[] aesKey = kgen.generateKey().getEncoded();
		new SecretKeySpec(aesKey, "AES");
		printByteArray("Unencrypted AES:", aesKey);
		Cipher pkCipher = Cipher.getInstance("RSA");

		PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
		pkCipher.init(Cipher.ENCRYPT_MODE, pk);
		CipherOutputStream cipherStream = new CipherOutputStream(out, pkCipher);
		cipherStream.write(aesKey);
		cipherStream.close();
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
