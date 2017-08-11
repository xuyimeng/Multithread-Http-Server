package edu.upenn.cis455.webserver;

import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class BlockingQueue {
	private Queue<Socket> queue;
	private int limit_size;
	
	public BlockingQueue(int limit){
		queue = new LinkedList<Socket>();
		this.limit_size = limit;
	}
	
	public synchronized void enQueue(Socket socket) throws InterruptedException{
		while(queue.size() == limit_size){
			wait();// distributed thread wait if blocking queue is full
		}
		queue.offer(socket);
		notify();
	}
	
	public synchronized Socket deQueue() throws InterruptedException{
		while(queue.size() == 0){
			wait(); // worker thread wait until the queue is not empty
		}
		Socket socket = queue.poll();
		notify();
		return socket;
	}
}
