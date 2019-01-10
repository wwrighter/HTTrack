import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTrackURL {
	
	private String protocol = "";
	private String hostname = "";
	private String fullpathname = "";
	private String pathname = "";
	private String filename = "";
	
	private int port = 80;
	
	public HTTrackURL(String aUrl) throws IllegalStateException, NumberFormatException {
		
		Pattern tPattern = Pattern.compile("(https?://)([^:^/]*):?(\\d*)?(.*)?");
		try {
			Matcher tMatch = tPattern.matcher(aUrl);
			tMatch.find();
			
			this.protocol = tMatch.group(1);
			this.hostname = tMatch.group(2);
			this.fullpathname = tMatch.group(4);
			this.pathname = this.fullpathname.substring(0, this.fullpathname.lastIndexOf('/') + 1);
			this.filename = new File(this.fullpathname).getName();	
			this.port = Integer.parseInt(tMatch.group(3));
			
		} catch (IllegalStateException e) {
			System.err.println("Invalid URL Specified.");
		} catch (NumberFormatException e) {
			this.port = 80;
		}
	}
	
	public String getProtocol() {
		return this.protocol;
	}
	
	public String getHostname() {
		return this.hostname;
	}
	
	public String getFullPathname() {
		return this.fullpathname;
	}
	
	public String getPathname() {
		return this.pathname;
	}
	
	public String getFilename() {
		return this.filename;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String toString() {
		String tStr = "";
		tStr += "Protocol: " + this.protocol + "\n";
		tStr += "Hostname: " + this.hostname + "\n";
		tStr += "Port: " + this.port + "\n";
		tStr += "Full Pathname: " + this.fullpathname + "\n";
		tStr += "Pathname: " + this.pathname + "\n";
		tStr += "Filename: " + this.filename + "\n";
		return tStr;
	}
}