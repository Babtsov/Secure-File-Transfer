import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

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
        private String command;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        private void sendPublicKey() {
        	StringBuilder messageHeader = new StringBuilder();
        	messageHeader.append("PUBLIC KEY\n");
        	File publicKeyFile = new File("public.der");
        	long keyLength = publicKeyFile.length();
        	messageHeader.append("LENGTH: "+keyLength+"\n");
        	try {
	        	FileReader publicKeyStream;
				publicKeyStream = new FileReader(publicKeyFile);
				char[] key = new char[(int) keyLength];
	        	publicKeyStream.read(key);
	        	publicKeyStream.close();
	        	out.println(messageHeader.toString() + new String(key));
	        	System.out.println("public key sent to client.");
			} catch (IOException e) {
				e.printStackTrace();
				sendErrorMessage("PUBLIC KEY FAIL");
				System.out.println("failed sending public key.");
			}
        
        }
        private void fileTransferHandler(){
        	
        }
        private void sendErrorMessage(String msg){
        	out.println("ERROR\n"+msg);
        }
        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                command = in.readLine();
                switch(command){
	                case "GET PUBLIC KEY":
	                	sendPublicKey();
	                	break;
	                case "ACCEPT FILE":
	                	//fileTransferHandler();
	                default:
	                	sendErrorMessage("INVALID COMMAND");
                }
            in.close();
            out.close();
            socket.close();
            } catch (IOException e) {
                System.out.println(e); 
            }
            
        }
    }
}
