package edu.upenn.cis455.webserver;

import java.io.OutputStream;
import java.io.PrintWriter;

public class HttpPrintWriter extends PrintWriter {
	
	private StringBuffer header;
	private StringBuffer content;
	private int bufferSize;
	public boolean isCommitted;
	
	public HttpPrintWriter(OutputStream out) {
		super(out);
		header = new StringBuffer();
		content = new StringBuffer();
		bufferSize = 100000;
		isCommitted = false;
	}
	
	public int getBufferSize(){
		return bufferSize;
	}
	
	public void setBufferSize(int size){
		this.bufferSize = size;
	}
	
	public void setHeader(StringBuffer header){
		this.header = header;
	}
	
	public void addHeader(String str){
		header.append(str);
	}
	
	public void setContent(StringBuffer content){
		this.content = content;
	}
	
	public void println(String addContent){
		// servlet add string to content body
		content.append(addContent);
		if(content.toString().getBytes().length >= bufferSize){
			flush();
		}
//		flush();
		
	} 
	
//	public void println(String a){
//		System.out.println(a);
//	}
	
	public void reset(){
		header = new StringBuffer();
		content = new StringBuffer();
	}

	public void flush(){
		if(!isCommitted){
			//super.print(header.toString() + "\r\n");
			super.print(header.toString());
			System.out.print(header.toString());
		}
		super.print(content.toString());
		System.out.print(content.toString());
		super.flush();
	}

}
