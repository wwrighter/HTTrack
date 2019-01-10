/* ----------------------------------------------------------- 
 * @HTTrack
 * @author: William Wright
 * @email : williamfzwright@outlook.com
 ----------------------------------------------------------- */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class HTTrack {

	private static String HTTPVersion = "1.0";
	private static final String HEADER_DELIM = "\r\n\r\n";
	public static Vector<String> validObjectNames;
	public static boolean processedBasePage = false;
	protected static String defaultHostname;
	protected static String defaultBasePath;
	private static File basepageFile = null;
	private static int objectIterator;

	protected static HTTrackURL url;

	public HTTrack(String aObjectName) {
	}
	// ----------------------------------------------------------------------------------------
	//						main
	// ----------------------------------------------------------------------------------------

	public static void main(String[] args) throws NullPointerException {
		
		validObjectNames = new Vector<String>();
			if(processedBasePage == false) {
				url = args.length > 0 ? new HTTrackURL(args[0]) : new HTTrackURL("http://localhost:80/testing/life/and/stuff/index.html");
				defaultBasePath = url.getPathname();
				defaultHostname = url.getHostname();
				
				try {
					fetchObject(composeGETString(url, HTTPVersion));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			for(objectIterator = 0; objectIterator < validObjectNames.size(); objectIterator++) {
				url = new HTTrackURL(url.getProtocol() + url.getHostname() + ":" + url.getPort() + defaultBasePath + validObjectNames.get(objectIterator));
					
					try {
						fetchObject(composeGETString(url, HTTPVersion));
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				System.out.println("Successfully copied website.");
	}

	// ----------------------------------------------------------------------------------------
	//						fetchObject
	// ----------------------------------------------------------------------------------------

	public static void fetchObject(String REQString) throws UnknownHostException, IOException {
		Map<String, String> headers = new HashMap<String, String>();
		byte[]  bHeaderDelim = HEADER_DELIM.getBytes();
		boolean headerfound = false;

		try(Socket socket = new Socket(url.getHostname(), url.getPort())) {
			socket.setSoTimeout(60000);

			InputStream rIn = socket.getInputStream();
			rIn = new BufferedInputStream(rIn);

			OutputStream rOut = socket.getOutputStream();
			rOut = new BufferedOutputStream(rOut);

			Writer cOut = new OutputStreamWriter(rOut, "US-ASCII");
			cOut.write(REQString);
			cOut.flush();

			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			int ch = 0;

			while((ch = rIn.read()) != -1) {
				if (ch == bHeaderDelim[0] && headerfound == false) {
					rIn.mark(HEADER_DELIM.length());
					byte []b = new byte[HEADER_DELIM.length()-1];
					if (rIn.read(b) == (HEADER_DELIM.length()-1) && b[0] == '\n' && b[1] == '\r' && b[2] == '\n') {
						buf.write(ch);
						buf.write(b);
						byte rawHeader[] = buf.toByteArray();
						int len = rawHeader.length;	
						if(len > 0) {
							headers = parseHeader(new String(rawHeader, 0, len-4, "US-ASCII"));
							if(headers.get("HTTPStatus").contains("404"))
								throw new HTTPException();
							headerfound = true;
						}
						buf.reset();
						continue;
					}
					else {
						rIn.reset();
					}
				}
				buf.write(ch);
			}
			byte rawPayload[] = buf.toByteArray();
			rIn.close();
			rOut.close();
			cOut.close();
			if(rawPayload.length > 0) {
				writeToFile(rawPayload, url.getPathname(), socket);
			}
			socket.close();
		} catch( HTTPException e ) {
			if(processedBasePage)
				purgeLinkRef(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// ----------------------------------------------------------------------------------------
	//						purgeLinkRef
	// ----------------------------------------------------------------------------------------
	public static void purgeLinkRef(HTTrackURL aUrl) {
		String currentBasePageContents;
		if(basepageFile.canRead()) {
			currentBasePageContents = readFileToString(basepageFile);
			currentBasePageContents = currentBasePageContents.replace(validObjectNames.get(objectIterator), "");
			writeStringToFile(currentBasePageContents, basepageFile);
		}
	}
	
	// ----------------------------------------------------------------------------------------
	//						writeStringToFile
	// ----------------------------------------------------------------------------------------
	public static void writeStringToFile(String newFileContentString, File aFile) {
		FileWriter fw;
		try {
			fw = new FileWriter(aFile.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(newFileContentString);
			bw.close();
		} catch (IOException e) {
			System.out.println("HTTrack.java: Error writing payload to file.");
		}
	}
	
	// ----------------------------------------------------------------------------------------
	//						readFileToString
	// ----------------------------------------------------------------------------------------
	public static String readFileToString(File aFile) {
		String contents="";
		
	 try(BufferedReader br = new BufferedReader(new FileReader(aFile))) {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            sb.append(line);
	            sb.append(System.lineSeparator());
	            line = br.readLine();
	        }
	        contents = sb.toString();
	    } catch (IOException e) {
			e.printStackTrace();
		}
	return contents;
	}
	// ----------------------------------------------------------------------------------------
	//						composeGETString
	// ----------------------------------------------------------------------------------------

	public static String composeGETString(HTTrackURL aURL, String aHTTPVersion) {
		String tStr = "";
		tStr += "GET " + aURL.getPathname() + aURL.getFilename() + " HTTP/" + HTTPVersion + "\r\n\r\n";
		return tStr;
	}
	// ----------------------------------------------------------------------------------------
	//						parseHeader
	// ----------------------------------------------------------------------------------------
	public static Map<String, String> parseHeader(String aString) throws NumberFormatException {
		Map<String, String> headers = new HashMap<String, String>();
		String[] pairs = aString.split("\n");
		if(pairs.length > 0)
			headers.put("HTTPStatus",  pairs[0]);
		for (int i=0; i < pairs.length; i++)  {
			String[] keyValue = pairs[i].toString().split(":", 2);
			// Assume the first response from a valid web server is a status code
			if(keyValue.length == 2)
				headers.put(keyValue[0], keyValue[1]);
		}
		return headers;
	}
	// ----------------------------------------------------------------------------------------
	//						writeToFile
	// -----------------------------------------------------------------------------------
	public static void writeToFile(byte[] aByteArray, String fileName, Socket aSocket) throws IOException {
		// Process the first few bytes of the payload to determine the encoding to use
		// Magic numbers for gif -> Hex: 47 49 46 38

		String filePathToWrite = url.getFullPathname().replace(defaultBasePath, "");
		
		boolean writeBytes = false;
		FileOutputStream fos;

		String gifMagicNumbers = "0x47, 0x49, 0x46, 0x38";
		byte[] gifMagicWords = "GIF89".getBytes();

		String[] hexValues = gifMagicNumbers.split(",");
		byte hexByte;

		int i;
		for(i = 0; i < hexValues.length; i++) {
			hexByte = Integer.valueOf(hexValues[i].substring(3).trim(), 16).byteValue();
			if(aByteArray[i] == hexByte || aByteArray[i] == gifMagicWords[i])
				continue;
			else
				break;
		}
		if(i == hexValues.length || i == gifMagicWords.length)
			writeBytes = true;

		String dataToWrite;

		if(writeBytes == true)  {		
			if(processedBasePage == false)
				processedBasePage = true;

			File file = new File(url.getHostname() + File.separator + filePathToWrite);
			file.getParentFile().mkdirs();

			try {
				fos = new FileOutputStream(file);
				fos.write(aByteArray);
				fos.close();
			} catch (IOException e) {
				System.out.println("HTTrack.java: Error writing payload to file.");
			}
		}
		else {
			try {
				dataToWrite = new String(aByteArray, 0, aByteArray.length, "US-ASCII");
				// --------------------------------------------------------------------------
				// Establish new objects from base page only
				// --------------------------------------------------------------------------
				File file = new File(url.getHostname() + File.separator + filePathToWrite);
				file.getParentFile().mkdirs();
				writeStringToFile(dataToWrite, file);
				if(processedBasePage == false) {
					basepageFile = file;
					processedBasePage = true;
					validObjectNames = parseLinks(dataToWrite);
				}
			} catch (UnsupportedEncodingException e) {
				System.out.println("HTTrack.java: Error writing payload to file.");
			}	
		}
		if(processedBasePage == false)
			processedBasePage = true;
	}
	// ----------------------------------------------------------------------------------------
	//						parseLinks
	// ----------------------------------------------------------------------------------------
	public static Vector<String> parseLinks(String aHTMLString) {
		HTTrackURL tempURL;
		final String HTML_A_TAG_PATTERN = "(?i)<a([^>]+)>(.+?)</a>";
		final String HTML_A_HREF_TAG_PATTERN = 
				"\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";
		Pattern tagPattern = Pattern.compile(HTML_A_TAG_PATTERN);
		Pattern linkPattern = Pattern.compile(HTML_A_HREF_TAG_PATTERN);
		Matcher tagMatcher = tagPattern.matcher(aHTMLString);
		Vector<String> result = new Vector<String>();

		while (tagMatcher.find()) {
			String href = tagMatcher.group(1); 
			Matcher linkMatcher = linkPattern.matcher(href);
			while (linkMatcher.find()) {
				String link = linkMatcher.group(1); 
				link = link.replaceAll("\"", "");
				
				if(link.contains("http://")) {
					tempURL = new HTTrackURL(link);
					if(tempURL.getHostname() != defaultHostname)
						continue;
				}
				result.add(link);
			}
		}
		return result;
	}
}
