package edu.upenn.cis455.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

public class HttpServer {
	
	public static int portNum;
	public static String rootDir;
	public static String HostName;
	public static String webFilePath;
	private static Handler h;
	private static BlockingQueue bq;
	private static ThreadPool threadPool;
	private static ServletContainer servletContainer;
	private static ServerSocket serverSocket = null;
	public static final int NUM_THREAD = 30;
	public static final int QUEUE_SIZE = 1000;
	public static volatile boolean running = true;
	public static HashMap<String ,FakeSession> sessionMap;
	
	public static void main(String args[])
	{
	  // if invoke without any command line argument
		if(args.length <= 2){
		  System.out.println("YimengXu (xuyimeng)"); 
		  System.exit(1);
		}
	  
		portNum = Integer.parseInt(args[0]);
		HostName = "localhost:"+portNum;
		rootDir = args[1];
		webFilePath = args[2];
		sessionMap = new HashMap<String, FakeSession>();
		
		try{
			serverSocket = new ServerSocket(portNum);
			System.out.println("Server on "+portNum+" is started...");
			
			bq = new BlockingQueue(QUEUE_SIZE);
			servletContainer = new ServletContainer();
			threadPool = new ThreadPool(bq,servletContainer);
			acceptRequests(serverSocket);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private static void acceptRequests(ServerSocket serverSocket) {
		while(running){
			
			Socket s;
			try {
				s = serverSocket.accept();
				threadPool.addSocket(s);
			} catch (IOException e) {
				
			} catch (Exception e) {
				
			}
		}
		try {
			threadPool.joinAll();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void shutdownAll(){
		try {
			// shutdown all worker thread first
			threadPool.shutdownAll();
			//shutdown main thread
			running = false;
			//shutdown main thread;
			serverSocket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static FakeSession getSession(String id)
	{
		if(sessionMap.containsKey(id)){
			return sessionMap.get(id);
		}
		return null;
	}
	public static void addSession(String id, FakeSession session)
	{
		sessionMap.put(id,session);
	}
}
