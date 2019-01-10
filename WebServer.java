import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {
	public static final String HTTP_OK = "200 OK", HTTP_NOTFOUND = "404 Not Found", HTTP_BADREQUEST = "400 Bad Request";
	private File wsBaseDir = null;
	public InputStream cIS;

	public static void main(String[] args) {
		System.out.println("WebServer HTTPD (C) 2014");

		try {
			if(args.length > 0) 
				new WebServer(Integer.parseInt(args[0]));
			else
				new WebServer(80);
		} catch (NumberFormatException | IOException e) {
			System.out.println("Unable to start WebServer: \n" + e);
		}
	}

	public WebServer(int aPortNum) throws IOException {
		ExecutorService executor = Executors.newFixedThreadPool(100);
		while(true) {
			try (final ServerSocket wsSocket = new ServerSocket(aPortNum)){
				this.wsBaseDir = new File("");
				executor.execute(new HTTPSession(wsSocket.accept()));
			} catch (IOException e) {
				System.out.println("Server failed to initialize.\n" + e);
			}
		}
	}

	public class HTTPSession implements Runnable {
		OutputStream cOut = null;
		Socket cSock = null;
		public HTTPSession(Socket aSocketInst) throws IOException {
			this.cOut = aSocketInst.getOutputStream();
			this.cSock = aSocketInst;
		}
		@Override
		public void run() {
			System.out.println("Serving " + this.cSock);
			processRequest(this.cSock);
		}


		private void sendResponse(Properties wsHeaders, String wsStatus, InputStream dataForClient ) throws IOException {
			PrintWriter ccOut = new PrintWriter(cOut);
			ccOut.print("HTTP/1.0 " + wsStatus + " \r\n");
			ccOut.print("\r\n");
			ccOut.flush();
			if ( dataForClient != null )
			{
				byte[] buff = new byte[2048];
				while (true)
				{
					int read = dataForClient.read( buff, 0, 2048 );
					if (read <= 0)
						break;
					cOut.write( buff, 0, read );
				}
			}
			cOut.flush();
			cOut.close();
			if ( dataForClient != null )
				dataForClient.close();
		}

		private void processRequest(Socket aClientSocket) {
			String method = "", uri = "", version = "";
			File newFile = null;
			try(BufferedReader cIn = new BufferedReader(new InputStreamReader(aClientSocket.getInputStream()))) {
				StringTokenizer stIn = new StringTokenizer(cIn.readLine());

				if(!stIn.hasMoreTokens() || stIn.countTokens() != 3) {
					sendResponse(null, HTTP_BADREQUEST, null);
				} else {
					method = stIn.nextToken();
					uri = stIn.nextToken();
					version = stIn.nextToken();

					uri = uri.trim().replace('/', File.separatorChar);

					// Remove leading slash to ensure we can access the correct directory
					int check = uri.indexOf(File.separator);

					if(check == 0)
						uri = uri.substring(1);

					newFile = new File(wsBaseDir.getCanonicalPath(), uri);
					
					if(!newFile.exists())
						sendResponse(null, HTTP_NOTFOUND, null);

					FileInputStream fis = new FileInputStream(newFile);
					sendResponse(null, HTTP_OK, fis);

				}
			}catch (FileNotFoundException e) {
				// System.out.println("Webserver : Could not open requested file: " + e);
			} catch (NullPointerException e) {
				// System.out.println("Webserver : Could not open requested file: " + e);
			} catch (IOException e) {
				System.err.println("WebServer : Unable to establish appropriate connection to client input stream.");
			} 
		}

	};
}
