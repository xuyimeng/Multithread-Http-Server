package edu.upenn.cis455.webserver;


import java.io.IOException;
import java.net.Socket;

public class ThreadPool{
	
	private BlockingQueue bq;
	private WorkerThread[] wThreads;
	private boolean isStopped = false;
	private ServletContainer sc;
	
	//Constructor
	public ThreadPool(BlockingQueue bQueue, ServletContainer sc){
		this.bq = bQueue;
		this.sc = sc;
		this.wThreads =  new WorkerThread[HttpServer.NUM_THREAD];
		
		for(int i=0; i < HttpServer.NUM_THREAD; i++){
			String threadName = "Thread " + i;
			try {
				wThreads[i] = new WorkerThread(threadName,bq,sc,this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for(WorkerThread thread : wThreads){
			thread.start();
		}
	}
	
	public synchronized void addSocket(Socket socket) throws Exception {
		if(this.isStopped){
			throw new IllegalStateException("ThreadPool is stopped");
		}
		this.bq.enQueue(socket);
		System.out.println("Server put request to queue...");
	}
	
	public synchronized void shutdownAll() throws IOException{
		this.isStopped = true;
		for(WorkerThread thread : wThreads){
			thread.shutdown();
		}
		for(WorkerThread thread : wThreads){
			System.out.println(thread.getName() + thread.getState());
		}
	}
	
	public synchronized void joinAll() throws InterruptedException{
		for(WorkerThread thread : wThreads){
			thread.join();
		}
	}
	
	public String generateControlPanel(){
		StringBuilder panelSb = new StringBuilder();
		panelSb.append("<html><body>");
		panelSb.append("<h1>HTTP Server Control Panel</h1>");
		panelSb.append("<h3>Yimeng Xu(SEAS Account: xuyimeng)</h3>");
		// to show the name and status of all the threads
		panelSb.append("<ul>");
		for(WorkerThread thread : wThreads){
			panelSb.append("<li>"+ thread.getName() +" "+ thread.getState()+"</li>");
		}
		panelSb.append("</ul>");
		//A button that shuts down the server
		panelSb.append("<a href=/shutdown><button type=\"button\">Shut Down Server</button></a>");
		panelSb.append("</body></html>");
		return panelSb.toString();
	}
	
}
	
	
