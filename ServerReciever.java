import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


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

        private void sendPublicKey() throws UnsupportedEncodingException, IOException, InterruptedException {
        	StringBuilder messageHeader = new StringBuilder();
        	messageHeader.append("PUBLIC KEY\n");
        	File publicKeyFile = new File("public.der");
        	long keyLength = publicKeyFile.length();
        	messageHeader.append(keyLength+"\n\n");
        	try {
        		System.out.println(out.getClass().toString());
        		BufferedInputStream publicKeyStream;
				publicKeyStream = new BufferedInputStream(new FileInputStream(publicKeyFile));
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
        private void sendErrorMessage(String msg) throws UnsupportedEncodingException, IOException{
        	msg = "ERROR\n" + msg + "\n\n";
        	out.write(msg.getBytes("ASCII"));
        }
        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedInputStream(socket.getInputStream());
                out = new BufferedOutputStream(socket.getOutputStream());
                ArrayList<String> headerParts = ProtocolUtilities.consumeAndBreakHeader(in);
                String command = headerParts.get(0);
                switch(command){
	                case "GET PUBLIC KEY":
	                	sendPublicKey();
	                	System.out.println("Sent!");
	                	break;
	                case "ACCEPT FILE":
	                default:
	                	//sendErrorMessage("INVALID COMMAND");
                }
            } catch (IOException | InterruptedException e) {
                System.out.println(e); 
                System.out.println("Server Error!!!");
            }
            
        }
    }
}
