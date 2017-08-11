package edu.upenn.cis455.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class HttpRequest {
	
	public BufferedReader br;
	public String reqMethod;
	public String filePath;
	public String httpVersion;
	public boolean isBadReq;
	public boolean isShutdown;
	public boolean isControl;
	public String  URI;
	public String  servletPath;
	public String  pathInfo;
	public String  queryString;
	public boolean isServletRequest;
	public boolean isModifiedHeader;
	public boolean isUnModifiedHeader;
	public boolean hasContent;
	public String reqHeaderStr;
	public String reqContentStr;
	public String message;
	public int contentLen;
	public int status_code;
	public HashMap<String,List<String>> reqHeaderMap;
	public HashMap<String,String> servletUrlMap;
	
	public HttpRequest( BufferedReader br, HashMap<String,String> servletUrl) throws IOException{
		this.br = br;
		isBadReq = false;
		isShutdown = false;
		isControl = false;
		isModifiedHeader = false;
		isUnModifiedHeader = false;
		hasContent = false;
		reqHeaderMap = new HashMap<String,List<String>>();
		servletUrlMap = servletUrl;// a map from servlet URL to servlet name
		reqHeaderStr = readReqHeader();
		parseRequestHeader(reqHeaderStr);
		if(!isBadReq) checkStatus();
		if(hasContent) reqContentStr = readReqContent();
	}
	
	private String readReqHeader() throws IOException{
		// split the request into separate lines
		String line = "";
		String reqStr = "";
		//read request from buffered readers
		
		while((line = br.readLine())!= null && line.length() != 0){
			reqStr += line + "\n";
		}
		System.out.println(reqStr);
		return reqStr;
	}
	
	private String readReqContent() throws IOException{
		char[] reqContent = new char[contentLen];
		br.read(reqContent,0,contentLen);
//		return reqContent.toString();
		return new String(reqContent);
	}
	
	private void parseRequestHeader(String request){
		if(request == null || request.length() == 0){
			this.isBadReq = true;
			message = "Request length not correct";
			status_code = 400;
		}
		String[] lines = request.split("\n"); 
		if(lines.length <= 0){
			this.isBadReq = true;
			message = "Request length not correct";
			status_code = 400;
		}else{
			parseInitialLine(lines[0]);
			String[] headerLines = new String[lines.length - 1]; 
			System.arraycopy(lines, 1, headerLines, 0, headerLines.length);
			if(headerLines.length != 0){
				parseHeader(headerLines);
			}
		}
	}
	private void parseInitialLine(String initLine){
		String[] reqStructure = initLine.split(" ");
		if(reqStructure.length != 3){
			this.isBadReq = true;
			message = "Initial request line wrong arguments number";
			status_code = 400;
			return;
		}
		reqMethod = reqStructure[0];
		filePath = reqStructure[1];
		httpVersion = reqStructure[2];
	}
	
	private void parseHeader(String[] headerLines){
		for(String header:headerLines){
			String[] headerPair = header.split(":",2);
			String key = headerPair[0].toLowerCase().trim();
			String val = headerPair[1].trim();
			if(reqHeaderMap.containsKey(key)){
				List<String> temp = reqHeaderMap.get(key);
				temp.add(val);
				reqHeaderMap.put(key,temp);
			}else{
				List<String> temp = new ArrayList<String>();
				temp.add(val);
				reqHeaderMap.put(key, temp);
			}
		}
	}
	
	private String simplifyPath(String path){
		
		StringBuilder pathSb = new StringBuilder();
		Stack<String> dirStack = new Stack<String>();
		String[] dirs = path.split("/");
		
		for(int i = 0; i < dirs.length; i++){
			if(dirs[i].equals(".") || dirs[i].equals("")){
				continue;
			}else if(dirs[i].equals("..")){
				if(!dirStack.isEmpty()){
					dirStack.pop();
				}else{
					isBadReq = true;
					message = "Invalid file path";
					status_code = 403;
					return null;
				}
			}else{
				dirStack.push(dirs[i]);
			}
		}
		for(String dir : dirStack){
			pathSb.append("/");
			pathSb.append(dir);
		}

		return pathSb.toString();
	}
	
	private String extractFromAbsPath(String path,String hostName){
		String result = "";
		int hostIndex = path.indexOf(hostName);
		if(hostIndex < 0){
			return path;
		}
		String tempAds = path.substring(0,hostIndex);
		if(!(tempAds.equals("http://") || tempAds.equals("https://"))){
			this.isBadReq = true;
			message = "HTTP address not correct";
			status_code = 400;
			return null;
		}
		result = path.substring(hostIndex + hostName.length());
		return result;
	}
	
	private void checkStatus(){
		// check if the request method valid
		if(!(reqMethod.equals("GET")||
				 reqMethod.equals("HEAD")||
				 reqMethod.equals("POST"))){
				this.isBadReq = true;
				
				message = "Invalid request method";
				status_code = 501;
				return;
			}
		// check if http version right 
		if(!(httpVersion.equals("HTTP/1.0")||
			 httpVersion.equals("HTTP/1.1"))){
			this.isBadReq = true;
			message = "Invalid HTTP version";
			status_code = 501;
			return;
		}
		// check if the request has content
		if(reqMethod.equals("POST") && reqHeaderMap.containsKey("content-length")){
			hasContent = true;
			contentLen = Integer.parseInt(reqHeaderMap.get("content-length").get(0));
		}
		// check if valid file path absolute file path for http/1.1
		if(httpVersion.equals("HTTP/1.1")){
			// check if contains if-modified-since flag and if-unmodified-since flag
			if(reqHeaderMap.containsKey("If-Modified-Since")){
				this.isModifiedHeader = true;
			}
			if(reqHeaderMap.containsKey("If-Unmodified-Since")){
				this.isUnModifiedHeader = true;
			}
			// check if header contains host 
			if(!reqHeaderMap.containsKey("host")){
				this.isBadReq = true;
				message = "No Host: header received for HTTP/1.1";
				status_code = 400;
				return;
			}
			String hostName = reqHeaderMap.get("host").get(0);
			filePath = extractFromAbsPath(filePath,hostName);
		}
		if(filePath != null){
			filePath = simplifyPath(filePath);
		}
		// special file path
		if(filePath != null){
			if(filePath.equals("/shutdown")){
				isShutdown = true;
			}else if(filePath.equals("/control")){
				isControl = true;
			}
		}
		
		if(filePath != null){
			checkServletRequest();
		}

	}
	
	private void checkServletRequest(){
		System.out.println("Check servlet request");
	    int i  = filePath.indexOf('?');
	    
	    if( i != -1){
	    	URI = filePath.substring(0,i);
	    	queryString = filePath.substring(i+1);
	    }else{
	    	URI = filePath;
	    	queryString = null;
	    }

		int matchLength = 0;//counter to count the max matching length
		String matchServletPath = null;
		
		for(String servletPath : servletUrlMap.keySet()){
			// check for exact match
			if(URI.equals(servletPath)){
				matchServletPath = servletPath;
				matchLength = servletPath.length();
				break;
			}
			// check for prefix match
			else if(servletPath.endsWith("/*")){
				String newServletPath = servletPath.substring(0, servletPath.length() - 2);
				if(URI.startsWith(newServletPath)){
					if(newServletPath.length() > matchLength){
						matchLength = newServletPath.length();
						matchServletPath = newServletPath;
					}
				}
			}
		}
		if(matchServletPath != null){
			isServletRequest = true;
			servletPath = matchServletPath;
			pathInfo = URI.substring(matchServletPath.length());
		}
	}
}
