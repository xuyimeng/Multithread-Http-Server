package edu.upenn.cis455.webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import javax.imageio.IIOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

public class WorkerThread extends Thread{
	
	private String name;
	private ThreadPool pool;
	private BlockingQueue bq;
	private ServletContainer sc;
	private BufferedReader br; // read request
	private OutputStream os; // write response 
	private String root;
	private Socket socket;
	private byte[] response;
	private volatile boolean isStopped = false;
	private static HashMap<Integer,String> statusMap;

	
	public WorkerThread(String name,BlockingQueue bq,ServletContainer sc, ThreadPool pool) throws Exception{
		this.name = name;
		this.bq = bq;
		this.sc = sc;
		this.pool = pool;
		this.root = HttpServer.rootDir;
		
		statusMap = new HashMap<Integer,String>();
		statusMap.put(200,"OK");
		statusMap.put(301, "Moved Permanently");
		statusMap.put(304, "Not Modified");
		statusMap.put(400, "Bad Request");
		statusMap.put(401, "Unauthorized");
		statusMap.put(403, "Forbidden");
		statusMap.put(404, "Not Found");
		statusMap.put(500, "Internal Server Error");
		statusMap.put(501, "Not Implemented");
	}
	
	// run method function: read request and handle response
	public void run(){
		
		System.out.println(name + " started...");

		//get request string and give this string to HttpRequest class
		try{
			
			while(!isStopped){
				//get socket from blocking queue by dequeue method
				socket = bq.deQueue();
				this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				this.os = socket.getOutputStream();
				System.out.println(name + " is processing with the request..");
				
				// parse the request String in HttpRequest
				HttpRequest req = new HttpRequest(br,sc.h.m_servletUrls);
				
				if(req.isBadReq){
					// if request is bad request, generate error page
					response = generateResponse(req.httpVersion,req.message,req.status_code);
				}else if(req.isShutdown){
					Thread helperThread = new Thread(new Runnable(){
						public void run() {
							try {
								os.write(generateResponse("HTTP/1.1","Server Shut down",200));
							} catch (IOException e) {
								e.printStackTrace();
							}
							HttpServer.shutdownAll();
						}
					});
					helperThread.start();
					helperThread.join();
					return;
					
				}else if(req.isControl){
					String panel = pool.generateControlPanel();	
					StringBuilder responseBuilder = new StringBuilder();
					responseBuilder.append(req.httpVersion+" 200 OK\r\n");
					responseBuilder.append("Content-Length:"+panel.length()+"\r\n");
					responseBuilder.append("Content-Type: text/html\r\n");
					responseBuilder.append(getDateHeader());
					responseBuilder.append("Accept-Ranges: bytes\r\n");
					responseBuilder.append("\r\n");
					responseBuilder.append(panel);
					response = responseBuilder.toString().getBytes();
				}else if(req.isServletRequest){
					System.out.println("dipatch the request to servlet");
					String servlet = sc.h.m_servletUrls.get(req.servletPath);
					try {
						sc.dispatchRequest(socket, req, servlet);
					} catch (ServletException e) {
						e.printStackTrace();
					}
					return;
				}else{
					if(req.httpVersion.equals("HTTP/1.1")){
						os.write("HTTP/1.1 100 Continue\r\n\r\n".getBytes());
					}
					// pass the request to HttpResponse class for getting response
					HttpResponse res = new HttpResponse(req,root);
					if(res.isBadResponse){
						response = generateResponse(req.httpVersion,res.message,res.errorCode);
					}else{
						response = res.response;
					}
				}
			
				// send response to client
				
				os.write(response);
				os.flush();
				System.out.println("Response sent");
				
				// close socket
				os.close();
				br.close();
				socket.close();
				
			}	
		}catch(InterruptedException e){
			System.out.println(name + "Has been interrupted");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void shutdown() throws IOException{
		this.interrupt(); // if this thread is wait in dequeue,break out
		if(socket!= null){
			socket.close();
		}
		this.isStopped = true;
	}
	
	public byte[] generateResponse(String httpVersion,String message,int status_code){
	
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<h1>"+ httpVersion+" "+status_code+" "+statusMap.get(status_code)+"</h1>");
		sb.append("<h2>"+ message +"</h2>");
		sb.append("</body></html>");
		String badMsg = sb.toString();
		
		StringBuilder responseBuilder = new StringBuilder();
		responseBuilder.append(httpVersion +" "+status_code+" "+statusMap.get(status_code)+"\r\n");
		responseBuilder.append("Content-Length:"+badMsg.length()+"\r\n");
		responseBuilder.append("Content-Type: text/html\r\n");
		responseBuilder.append(getDateHeader());
		responseBuilder.append("Accept-Ranges: bytes\r\n");
		responseBuilder.append("\r\n");
		responseBuilder.append(badMsg);
		return responseBuilder.toString().getBytes();
	}
	
	public static String getDateHeader(){
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		Date date = new Date();
		String[] dateComp = date.toString().split(" ");
		String currentTime = "Date: ";
		currentTime += dateComp[0] + ", " + dateComp[2] + " " + dateComp[1]
				+ " " + dateComp[5] + " " + dateComp[3] + " " + dateComp[4];
		currentTime += "\r\n";
		return currentTime;
	}
	
	
}
