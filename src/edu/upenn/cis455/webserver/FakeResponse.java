package edu.upenn.cis455.webserver;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * @author tjgreen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class FakeResponse implements HttpServletResponse {
	
	private FakeRequest m_request;
	private FakeSession m_session;
	private List<Cookie> m_cookies;
	private String m_contentType;
	private int m_contentLength;
	private OutputStream m_os;
	private HttpPrintWriter m_writer;
	private String m_encoding;
	private int m_statusCode;
	private Map<String,StringBuffer> m_responseHeader; 
	private static final SimpleDateFormat m_datePattern = new SimpleDateFormat(
			"EEE, dd MM HH:mm:ss zzz yyyy");
	
	private static HashMap<Integer, String> statusMap;
	
	public FakeResponse(FakeRequest request,String sid ,OutputStream os){
		this.m_request = request;
		this.m_session = HttpServer.getSession(sid);
		this.m_cookies = new ArrayList<Cookie>();
		this.m_os = os;
		//this.m_writer = new HttpPrintWriter(os); //TODO
		this.m_writer = new HttpPrintWriter(os);
		this.m_responseHeader = new HashMap<String,StringBuffer>();
		this.m_statusCode = 200; // default status code
		setStatusMap();
	}
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#addCookie(javax.servlet.http.Cookie)
	 */
	public void addCookie(Cookie cookie) {
		m_cookies.add(cookie);
		// append new added cookie to header map
		long expireTime;
		if(cookie.getMaxAge() > 0  && m_session != null){
			expireTime = m_session.getCreationTime() + 1000 * cookie.getMaxAge();
		}else{
			expireTime = new Date().getTime() + 1000 * cookie.getMaxAge();
		}
		SimpleDateFormat format = new SimpleDateFormat("EEE, d-MMM-yyyy HH:mm:ss z");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		String expireTimeStr=format.format(new Date(expireTime));
		StringBuffer sb = new StringBuffer();
		sb.append(cookie.getName() + "=" + cookie.getValue() +
				"; expires=" + expireTimeStr);
		m_responseHeader.put("Set-Cookie",sb) ;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#containsHeader(java.lang.String)
	 */
	public boolean containsHeader(String arg0) {
		return m_responseHeader.containsKey(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#encodeURL(java.lang.String)
	 */
	public String encodeURL(String url) {
		return url;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#encodeRedirectURL(java.lang.String)
	 */
	public String encodeRedirectURL(String url) {
		return url;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#encodeUrl(java.lang.String)
	 */
	public String encodeUrl(String url) {
		return url;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#encodeRedirectUrl(java.lang.String)
	 */
	public String encodeRedirectUrl(String url) {
		return url;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#sendError(int, java.lang.String)
	 */
	public void sendError(int sc, String msg) throws IOException {
		if(isCommitted()){
			throw new IllegalStateException();
		}
		setStatus(sc);
		setContentType("text/html");
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<h1>"+m_request.getProtocol()+" Error: "+ sc +": "
		            + statusMap.get(sc)+"</h1>");
		sb.append("<h3>"+msg+"</h3>");
		sb.append("</body></html>");
		m_writer.setContent(new StringBuffer(sb));
		flushBuffer();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#sendError(int)
	 */
	public void sendError(int sc) throws IOException {
		if(isCommitted()){
			throw new IllegalStateException();
		}
		setStatus(sc);
		m_writer.println("Status Code: " + sc + "\r\n");
		flushBuffer();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#sendRedirect(java.lang.String)
	 */
	public void sendRedirect(String arg0) throws IOException {
		System.out.println("[DEBUG] redirect to " + arg0 + " requested");
		System.out.println("[DEBUG] stack trace: ");
		Exception e = new Exception();
		StackTraceElement[] frames = e.getStackTrace();
		for (int i = 0; i < frames.length; i++) {
			System.out.print("[DEBUG]   ");
			System.out.println(frames[i].toString());
		}
		m_writer.flush();
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#setDateHeader(java.lang.String, long)
	 */
	public void setDateHeader(String name, long date) {
		String dateStr = m_datePattern.format(new Date(date));
		m_responseHeader.put(name,new StringBuffer(dateStr));
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#addDateHeader(java.lang.String, long)
	 */
	public void addDateHeader(String name, long date) {
		String dateStr = m_datePattern.format(new Date(date));
		StringBuffer temp;
		if(m_responseHeader.containsKey(name)){
			temp = m_responseHeader.get(name).append(", " + dateStr);
		}else{
			temp = new StringBuffer(dateStr);
		}
		m_responseHeader.put(name, temp);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String arg0, String arg1) {
		m_responseHeader.put(arg0, new StringBuffer(arg1));
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#addHeader(java.lang.String, java.lang.String)
	 */
	public void addHeader(String key, String val) {
		StringBuffer temp;
		if(m_responseHeader.containsKey(key)){
			temp = new StringBuffer(val);
		}else{
			temp = m_responseHeader.get(key).append(", "+val);
		}
		m_responseHeader.put(key, temp);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#setIntHeader(java.lang.String, int)
	 */
	public void setIntHeader(String key, int val) {
		m_responseHeader.put(key, new StringBuffer(val));
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#addIntHeader(java.lang.String, int)
	 */
	public void addIntHeader(String key, int val) {
		StringBuffer temp;
		if(m_responseHeader.containsKey(key)){
			temp = new StringBuffer(val);
		}else{
			temp = m_responseHeader.get(key).append(", "+val);
		}
		m_responseHeader.put(key, temp);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#setStatus(int)
	 */
	public void setStatus(int sc) {
		m_statusCode = sc;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#setStatus(int, java.lang.String)
	 */
	public void setStatus(int arg0, String arg1) {
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#getCharacterEncoding()
	 */
	public String getCharacterEncoding() {
		return m_encoding == null ? "ISO-8859-1":m_encoding;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#getContentType()
	 */
	public String getContentType() {
		return m_contentType == null ? "text/html":m_contentType;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#getOutputStream()
	 */
	public ServletOutputStream getOutputStream() throws IOException {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#getWriter()
	 */
	public PrintWriter getWriter() throws IOException {
		if(m_os == null){
			throw new IllegalStateException();
		}
		return m_writer;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#setCharacterEncoding(java.lang.String)
	 */
	public void setCharacterEncoding(String arg0) {
		if(!isCommitted()){
			m_encoding = arg0;
			setHeader("Character-Encoding", arg0);
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#setContentLength(int)
	 */
	public void setContentLength(int len) {
		if(!isCommitted()){
			m_contentLength = len;
			setIntHeader("Content-Length",len);
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#setContentType(java.lang.String)
	 */
	public void setContentType(String type) {
		if(!isCommitted()){
			m_contentType = type;
			setHeader("Content-Type",type);
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#setBufferSize(int)
	 */
	public void setBufferSize(int size) {
		if(isCommitted()){
			throw new IllegalStateException();
		}
		m_writer.setBufferSize(size);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#getBufferSize()
	 */
	public int getBufferSize() {
		return m_writer.getBufferSize();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#flushBuffer()
	 */
	public void flushBuffer() throws IOException {
		if(isCommitted()) return;
		// write status line and header to the writer
		m_writer.addHeader(m_request.getProtocol()+" "+m_statusCode
				+" "+statusMap.get(m_statusCode)+"\r\n");
		for(String key : m_responseHeader.keySet()){
			m_writer.addHeader(key+":"+m_responseHeader.get(key)+"\r\n");
		}
		// Write session to the response header
		System.out.println(m_session.getId());
		m_writer.addHeader("Set-Cookie: "+"JSESSIONID="+m_session.getId()+"\r\n");
		
		m_writer.addHeader("\r\n");
		m_writer.flush();
		m_writer.close();
		m_writer.isCommitted = true;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#resetBuffer()
	 */
	public void resetBuffer() {
		m_writer = new HttpPrintWriter(m_os);                                                                                                                          
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#isCommitted()
	 */
	public boolean isCommitted() {
		return m_writer.isCommitted;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#reset()
	 */
	public void reset() {
		if(isCommitted()){
			throw new IllegalStateException();
		}
		setStatus(200);
		m_responseHeader = new HashMap<String, StringBuffer>();
		m_writer.reset();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#setLocale(java.util.Locale)
	 */
	public void setLocale(Locale arg0) {

	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#getLocale()
	 */
	public Locale getLocale() {
		return null;
	}
	
	public static void setStatusMap(){
		  FakeResponse.statusMap = new HashMap<Integer, String>();
		  FakeResponse.statusMap.put(100, "Continue");
		  FakeResponse.statusMap.put(101, "Switching Protocols");
		  FakeResponse.statusMap.put(200, "OK");
		  FakeResponse.statusMap.put(201, "Created");
		  FakeResponse.statusMap.put(202, "Accepted");
		  FakeResponse.statusMap.put(203, "Non-Authoritative Information");
		  FakeResponse.statusMap.put(204, "No Content");
		  FakeResponse.statusMap.put(205, "Reset Content");
		  FakeResponse.statusMap.put(206, "Partial Content");
		  FakeResponse.statusMap.put(300, "Multiple Choices");
		  FakeResponse.statusMap.put(301, "Moved Permanently");
		  FakeResponse.statusMap.put(302, "Found");
		  FakeResponse.statusMap.put(303, "See Other");
		  FakeResponse.statusMap.put(304, "Not Modified");
		  FakeResponse.statusMap.put(305, "Use Proxy");
		  FakeResponse.statusMap.put(307, "Temporary Redirect");
		  FakeResponse.statusMap.put(400, "Bad Request");
		  FakeResponse.statusMap.put(401, "Unauthorized");
		  FakeResponse.statusMap.put(402, "Payment Required");
		  FakeResponse.statusMap.put(403, "Forbidden");
		  FakeResponse.statusMap.put(404, "Not Found");
		  FakeResponse.statusMap.put(405, "Method Not Allowed");
		  FakeResponse.statusMap.put(406, "Not Acceptable");
		  FakeResponse.statusMap.put(407, "Proxy Authentication Required");
		  FakeResponse.statusMap.put(408, "Request Timeout");
		  FakeResponse.statusMap.put(409, "Conflict");
		  FakeResponse.statusMap.put(410, "Gone");
		  FakeResponse.statusMap.put(411, "Length Required");
		  FakeResponse.statusMap.put(412, "Precondition Failed");
		  FakeResponse.statusMap.put(413, "Request Entity Too Large");
		  FakeResponse.statusMap.put(414, "Request-URI Too Long");
		  FakeResponse.statusMap.put(415, "Unsupported Media Type");
		  FakeResponse.statusMap.put(416, "Requested Range Not Satisfiable");
		  FakeResponse.statusMap.put(417, "Expectation Failed");
		  FakeResponse.statusMap.put(500, "Internal Server Error");
		  FakeResponse.statusMap.put(501, "Not Implmented");
		  FakeResponse.statusMap.put(502, "Bad Gateway");
		  FakeResponse.statusMap.put(503, "Service Unavailable");
		  FakeResponse.statusMap.put(504, "Gateway Timeout");
		  FakeResponse.statusMap.put(505, "HTTP Version Not Supported");
	}
}
