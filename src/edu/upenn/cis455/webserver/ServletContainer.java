package edu.upenn.cis455.webserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class ServletContainer {
	
	private String webdotxml;
	public Handler h;
	private FakeContext fc;
	private HashMap<String,HttpServlet> servlets;
	
	public ServletContainer() throws Exception{
		this.webdotxml = HttpServer.webFilePath;
		this.h = parseWebdotxml();
		this.fc = createContext();
		this.servlets = createServlets();
	}
	
	private Handler parseWebdotxml() throws Exception {
		Handler h = new Handler();
		File file = new File(webdotxml);
		if (file.exists() == false) {
			System.err.println("error: cannot find " + file.getPath());
			System.exit(-1);
		}
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		parser.parse(file, h);
		return h;
	}
	
	private FakeContext createContext() {
		FakeContext fc = new FakeContext();
		for (String param : h.m_contextParams.keySet()) {
			fc.setInitParam(param, h.m_contextParams.get(param));
		}
		return fc;
	}
	
	private HashMap<String,HttpServlet> createServlets() throws Exception {
		HashMap<String,HttpServlet> servlets = new HashMap<String,HttpServlet>();
		for (String servletName : h.m_servlets.keySet()) {
			FakeConfig config = new FakeConfig(servletName, fc);
			String className = h.m_servlets.get(servletName);
			String classRoot = "edu.upenn.cis455.servlet.";
			Class servletClass = Class.forName(classRoot + className);
			HttpServlet servlet = (HttpServlet) servletClass.newInstance();
			HashMap<String,String> servletParams = h.m_servletParams.get(servletName);
			if (servletParams != null) {
				for (String param : servletParams.keySet()) {
					config.setInitParam(param, servletParams.get(param));
				}
			}
			servlet.init(config);
			servlets.put(servletName, servlet);
		}
		return servlets;
	}
	
	public void dispatchRequest(Socket socket, HttpRequest req,	
									String servletName) throws ServletException, IOException{
		
		FakeRequest request = new FakeRequest(socket,req,fc);
		FakeResponse response = new FakeResponse(request,request.m_sid,socket.getOutputStream());
		HttpServlet servlet = servlets.get(servletName);
		System.out.println("Found servlet: " + servletName);
		servlet.service(request,response);
	}
}
