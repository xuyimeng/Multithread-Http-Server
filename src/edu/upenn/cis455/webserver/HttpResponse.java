package edu.upenn.cis455.webserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class HttpResponse {
	
	private HttpRequest request;
	public byte[] response;
	public byte[] header;
	private String root;
	public boolean isBadResponse;
	public int errorCode;
	public String message;
	
	public HttpResponse(HttpRequest request, String root){
		this.request = request;
		this.root = root;
		isBadResponse = false;
		message = "";
		generateResponse();
	}

	public void generateResponse(){
		//initialize string builders
		StringBuilder statusSb = new StringBuilder();
		StringBuilder headerSb = new StringBuilder();
		StringBuilder responseSb = new StringBuilder();
		
		try {
			File file = new File(root + request.filePath);
			// check if modified/ if unmidifed 
			if(request.httpVersion.equals("HTTP/1.1") && request.reqMethod.equals("GET")){
				if(request.isModifiedHeader || request.isUnModifiedHeader)
					if(!checkFileModify(file,request)) {
						return;
					}
			}
			byte[] fileContent = getDirContent(file,request.filePath); // root + request.filePath  = the location of file
			
			// generate status
			statusSb.append(request.httpVersion+" 200 OK\r\n");//" 200 OK\r\n"
			headerSb.append("Content-Length:"+fileContent.length+" \r\n");
			headerSb.append("Content-Type: " + getContentType(file) + " \r\n");
			headerSb.append(WorkerThread.getDateHeader());
			headerSb.append("Accept-Ranges: bytes\r\n");
			headerSb.append("\r\n");
			
			// append status and header together
			responseSb.append(statusSb.toString());
			responseSb.append(headerSb.toString());
			header = responseSb.toString().getBytes();
			
			if(request.reqMethod.equals("GET")){
				response = new byte[header.length + fileContent.length];
				System.arraycopy(header, 0, response, 0, header.length);
				System.arraycopy(fileContent, 0, response, header.length, fileContent.length);
			}else if(request.reqMethod.equals("HEAD")){
				response = header;
			}
			System.out.println(responseSb.toString());
			
		} catch (FileNotFoundException e) {// file not found exception, status code 404
			System.out.println("file not found\n");
			isBadResponse = true;
			errorCode = 404;
			message = "File Not Found";
			return;
		} catch (Exception e) {//internal server error, status code 500
			isBadResponse = true;
			errorCode = 500;
			message = "Internal Server Error";
			return;
		}
	}
	
	private byte[] getDirContent(File file,String dirPath) throws IOException{
		if(file.isFile()){
			return readFileContent(file);
		}else{
			StringBuilder dirSb = new StringBuilder();
			String[] fileList = file.list();
			dirSb.append("<html><body>");
			dirSb.append("<h1> Content of files in directory "+dirPath+"</h1>");
			dirSb.append("<ul>");
			for(String subfile : fileList){
				String filePath = dirPath + "/"+subfile; 
				dirSb.append("<li>"+"<a href="+filePath+">"+subfile+"</a></li>");
			}
			dirSb.append("</ul>");
			dirSb.append("</body></html>");
			return dirSb.toString().getBytes();
		}
	}
	
	private byte[] readFileContent(File file) throws IOException{
		// read the file
		FileInputStream fis = new FileInputStream(file);
		// read content from file
		BufferedInputStream inputStream = new BufferedInputStream(fis);
		byte[] fileContent = new byte[(int) file.length()];
		inputStream.read(fileContent);
		//close the stream
		inputStream.close();
		fis.close();
		return fileContent;
	}
	
	private boolean checkFileModify(File file, HttpRequest req){
		long fileModifiedTime = file.lastModified();
		if(req.isModifiedHeader){
			long expectedModifiedTime = getTimeFromDate(req.reqHeaderMap.get("If-Modified-Since").get(0));
			if(expectedModifiedTime == -1){
				return true; // could not parse the header date, ignore the header
			}else{
				if(fileModifiedTime > expectedModifiedTime){		
					return true; // file modified since expected time return true
				}else{
					System.out.println("File not modified since expected time");
					String errorMessage = "HTTP/1.1 304 Not Modified\r\n";
					errorMessage += WorkerThread.getDateHeader();
					errorMessage += "\r\n";
					response = errorMessage.getBytes();
					return false;
				}
			}
		}
		if(req.isUnModifiedHeader){
			long expectedModifiedTime = getTimeFromDate(req.reqHeaderMap.get("If-Unmodified-Since").get(0));
			if(expectedModifiedTime == -1){
				return true; // could not parse the header date, ignore the header
			}else{
				if(fileModifiedTime < expectedModifiedTime){		
					return true; // file modified since expected time return true
				}else{
					System.out.println("File modified since expected time");
					String errorMessage = "HTTP/1.1 412 Precondition Failed\r\n";
					errorMessage += "\r\n";
					response = errorMessage.getBytes();
					return false;
				}
			}
		}
		return true;
	}
	
	private String getContentType(File file){
		String contentType = "text/html";
		if (file.canRead()) {
			if (!file.isDirectory()) {
				contentType = "text/plain";
				String fileName = file.getName().toLowerCase();
				
				if (fileName.endsWith(".txt"))
					contentType = "text/plain";
				else if (fileName.endsWith(".html"))
					contentType = "text/html";
				else if (fileName.endsWith(".jpg"))
					contentType = "image/jpeg";
				else if (fileName.endsWith(".gif"))
					contentType = "image/gif";
				else if (fileName.endsWith(".png"))
					contentType = "image/png";
			}
		}
		return contentType;
	}
	
	private long getTimeFromDate(String dateString){
		List<SimpleDateFormat> formatlist = new ArrayList<SimpleDateFormat>();
		formatlist.add(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"));
		formatlist.add(new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss zzz"));
		formatlist.add(new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy"));
		long expectedTime = -1;
		// Three different data formats.
		for(SimpleDateFormat format : formatlist){
			try {
				expectedTime = format.parse(dateString).getTime();
			} catch (ParseException e) {
				
			}
		}
		return expectedTime;
	}
}
