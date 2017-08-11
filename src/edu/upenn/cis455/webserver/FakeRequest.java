package edu.upenn.cis455.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author Todd J. Green
 */
public class FakeRequest implements HttpServletRequest {
	
	private Properties m_props = new Properties();
	private FakeSession m_session = null;
	private Socket m_socket;
	private String m_method;
	private HttpRequest m_request;
	private String m_protocal;
	private String m_URI;
	private String m_servletPath;
	private String m_pathInfo;
	private String m_queryString;
	private String m_encoding;
	private String m_content;
	private FakeContext m_fc;
	private HashMap<String, List<String>> m_reqHeader = new HashMap<String,List<String>>();
	private HashMap<String, List<String>> m_parameterMap = new HashMap<String,List<String>>();
	private boolean m_headerAccess;
	private List<Cookie> m_cookies = new ArrayList<Cookie>();
	private Locale m_locale;
	public String m_sid;
	
	public FakeRequest(){
		
	}
	
	public FakeRequest(FakeSession session) {
		m_session = session;
	}
	
	public FakeRequest(Socket socket,HttpRequest req,FakeContext fc) {
		// initialize following parameters to null
		this.m_sid = null;
		this.m_encoding = null;
		this.m_locale = null;
		this.m_session = null;
		this.m_fc = fc;
		// initialize parameter from request
		this.m_socket = socket;
		this.m_request = req;
		this.m_reqHeader = req.reqHeaderMap;
		this.m_headerAccess = true;
		this.m_method = req.reqMethod;
		this.m_protocal = req.httpVersion;
		this.m_URI = req.URI;
		this.m_servletPath = req.servletPath;
		this.m_pathInfo = req.pathInfo;
		this.m_queryString = req.queryString;
		this.m_content = req.reqContentStr;
		this.m_parameterMap = parseParamFromRequest();
		parseCookiesFromRequest();
	}
	
	private void parseCookiesFromRequest(){
		if(m_reqHeader.containsKey("cookie")){
			String cookiePairs[] = m_reqHeader.get("cookie").get(0).trim().split(";");
			for(String cookie : cookiePairs){
				String keyPair[] = cookie.split("=");
				String cookieKey = keyPair[0].trim();
				String cookieValue = keyPair[1].trim();
				if(cookieKey.equalsIgnoreCase("jsessionid")){
					System.out.println("hahaha");
					System.out.println(HttpServer.sessionMap.keySet());
					if(HttpServer.sessionMap.containsKey(cookieValue)){
						m_session = HttpServer.getSession(cookieValue);
					}
					m_sid = cookieValue;
				}else{
					m_session = new FakeSession(m_fc);
					HttpServer.addSession(m_session.getId(),m_session);
					m_sid = m_session.getId();
					System.out.println(m_sid);
				}
				Cookie tempCookie = new Cookie(cookieKey,cookieValue);
				m_cookies.add(tempCookie);
			}
		}else{
			m_session = new FakeSession(m_fc);
			HttpServer.addSession(m_session.getId(),m_session);
			m_sid = m_session.getId();
			System.out.println(m_sid);
		}
	}
	
	private HashMap<String,List<String>> parseParamFromRequest(){
		HashMap<String, List<String>> paraMap = new HashMap<String, List<String>>();
		if(!m_method.equals("POST")){
			return paraMap;
		}
		if(m_queryString != null){
			addParameter(m_queryString,paraMap);
		}
		if(m_content != null){
			addParameter(m_content,paraMap);
		}
		return paraMap;
	}
	
	private void addParameter(String str, HashMap<String,List<String>> map){
		String[] pairs = str.split("&");
		for(String pair : pairs){
			String[] keyValue = pair.split("=");
			String key = keyValue[0];
			String val = keyValue[1];
			if(map.containsKey(key)){
				List<String> vals = map.get(key);
				vals.add(val);
				map.put(key, vals);
			}else{
				List<String> vals = new ArrayList<String>();
				vals.add(val);
				map.put(key,vals);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getAuthType()
	 */
	public String getAuthType() {
		return BASIC_AUTH;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getCookies()
	 */
	public Cookie[] getCookies() {
		System.out.println("haha");
		System.out.println(m_cookies.get(0).getName() + " " + m_cookies.get(0).getValue());
		return m_cookies.toArray(new Cookie[m_cookies.size()]);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
	 */
	public long getDateHeader(String arg0) {
		arg0 = arg0.toLowerCase();
		if(!m_reqHeader.containsKey(arg0)){
			return -1;
		}
		List<SimpleDateFormat> formatlist = new ArrayList<SimpleDateFormat>();
		formatlist.add(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"));
		formatlist.add(new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss zzz"));
		formatlist.add(new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy"));
		long expectedTime = -1;
		String dateString = m_reqHeader.get(arg0).get(0);
		for(SimpleDateFormat format : formatlist){
			try {
				expectedTime = format.parse(dateString).getTime();
			} catch (ParseException e) {
				
			}
		}
		if(expectedTime == -1){
			throw new IllegalArgumentException();
		}else{
			return expectedTime;
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getHeader(java.lang.String)
	 */
	public String getHeader(String arg0) {
		arg0 = arg0.toLowerCase();
		if(m_reqHeader.containsKey(arg0)){
			return m_reqHeader.get(arg0).get(0);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
	 */
	public Enumeration<String> getHeaders(String arg0) {
		//container does not allow access to header information
		if(m_headerAccess = false)  return null;
		arg0 = arg0.toLowerCase();
		if(m_reqHeader.containsKey(arg0)){
			return Collections.enumeration(m_reqHeader.get(arg0));
		}else{
			//the request does not have any headers of that name 
			//return an empty enumeration
			return Collections.emptyEnumeration();
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
	 */
	public Enumeration<String> getHeaderNames() {
		//Returns an enumeration of all the header names this request contains
		//container does not allow access to header information
		if(m_headerAccess = false)  return null;
		if(m_reqHeader.isEmpty()){
			return Collections.emptyEnumeration();
		}else{
			return Collections.enumeration(m_reqHeader.keySet());
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
	 */
	public int getIntHeader(String arg0) throws NumberFormatException{
		arg0 = arg0.toLowerCase();
		if(!m_reqHeader.containsKey(arg0)){
			return -1;
		}
		return Integer.parseInt(m_reqHeader.get(arg0).get(0));
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getMethod()
	 */
	public String getMethod() {
		return m_method;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getPathInfo()
	 */
	public String getPathInfo() {
		if(m_pathInfo.equals("")) return null;
		return m_pathInfo;
	}
	
	public void setPathInfo(String path) {
		this.m_pathInfo = path;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
	 */
	public String getPathTranslated() {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getContextPath()
	 */
	public String getContextPath() {
		return "";//servlets in the default (root) context
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getQueryString()
	 */
	public String getQueryString() {
		if(m_queryString == null || m_queryString.length() == 0) {
			return null;
		}
		return m_queryString;	
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
	 */
	public String getRemoteUser() {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
	 */
	public boolean isUserInRole(String arg0) {
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	public Principal getUserPrincipal() {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
	 */
	public String getRequestedSessionId() {
		return m_session.getId();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getRequestURI()
	 */
	public String getRequestURI() {
		return m_URI;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getRequestURL()
	 */
	public StringBuffer getRequestURL() {
		StringBuffer sb = new StringBuffer();
		sb.append("http://");
		sb.append(getServerName()).append(":").append(getServerPort());
		sb.append(m_URI);
		return sb;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getServletPath()
	 */
	public String getServletPath() {
		return m_servletPath;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
	 */
	public HttpSession getSession(boolean create) {
		if (create) {
			if (! hasSession()) {
				// generate a new fake session
				m_session = new FakeSession(m_fc);
				HttpServer.addSession(m_session.getId(),m_session);
				System.out.println("new session created");
				return m_session;
			}
		} else {
			if (! hasSession()) {
				m_session = null;
			}
		}
		return m_session;
	}
	
	private boolean isSessionExpired(long curt, long last){
		if((curt-last) < m_session.getMaxInactiveInterval()*1000){
			return false;
		}else{
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getSession()
	 */
	public HttpSession getSession() {
		return getSession(true);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
	 */
	public boolean isRequestedSessionIdValid() {
		if(HttpServer.sessionMap.containsKey(getRequestedSessionId())){
			return m_session.isValid();
		}
		else return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
	 */
	public boolean isRequestedSessionIdFromCookie() {
		return true;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
	 */
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
	 */
	public boolean isRequestedSessionIdFromUrl() {
		if(m_reqHeader.containsKey("session")){
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String arg0) {
		return m_props.get(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getAttributeNames()
	 */
	public Enumeration getAttributeNames() {
		return m_props.keys();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 */
	public String getCharacterEncoding() {
		return m_encoding == null ? "ISO-8859-1" : m_encoding;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
	 */
	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		m_encoding = arg0;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getContentLength()
	 */
	public int getContentLength() {
		if(m_reqHeader.containsKey("content-length")){
			return Integer.parseInt(m_reqHeader.get("content-length").get(0));
		}
		return -1;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getContentType()
	 */
	public String getContentType() {
		if(m_reqHeader.containsKey("content-type")){
			return m_reqHeader.get("content-type").get(0);
		}
		return "text/html";
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getInputStream()
	 */
	public ServletInputStream getInputStream() throws IOException {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
	 */
	public String getParameter(String arg0) {
		if(!m_parameterMap.containsKey(arg0)){
			return null;
		}
		return m_parameterMap.get(arg0).get(0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameterNames()
	 */
	public Enumeration<String> getParameterNames() {
		if(m_parameterMap.size() == 0){
			return Collections.emptyEnumeration();
		}
		return Collections.enumeration(m_parameterMap.keySet());
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
	 */
	public String[] getParameterValues(String arg0) {
		if(!m_parameterMap.containsKey(arg0)){
			return null;
		}
		String[] tempArray = new String[m_parameterMap.get(arg0).size()];
		return  m_parameterMap.get(arg0).toArray(tempArray);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameterMap()
	 */
	public Map<String,String[]> getParameterMap() {
		Map<String,String[]> paraMap = new HashMap<String,String[]>();
		for(String key : m_parameterMap.keySet()){
			String[] tempArray = new String[m_parameterMap.get(key).size()];
			paraMap.put(key,m_parameterMap.get(key).toArray(tempArray));
		}
		return paraMap;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getProtocol()
	 */
	public String getProtocol() {
		return m_protocal; 
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getScheme()
	 */
	public String getScheme() {
		return "http";
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getServerName()
	 */
	public String getServerName() {
		return "localhost";
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getServerPort()
	 */
	public int getServerPort() {
		return HttpServer.portNum;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getReader()
	 */
	public BufferedReader getReader() throws IOException {
		// TODO test this!
		InputStreamReader is = new InputStreamReader(m_socket.getInputStream(), m_encoding);
		BufferedReader bufferedReader = new BufferedReader(is);
		return bufferedReader;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getRemoteAddr()
	 */
	public String getRemoteAddr() {
		return m_socket.getRemoteSocketAddress().toString();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getRemoteHost()
	 */
	public String getRemoteHost() {
		return m_socket.getInetAddress().getHostName();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String arg0, Object arg1) {
		m_props.put(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String arg0) {
		m_props.remove(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocale()
	 */
	public Locale getLocale() {
		return m_locale;
	}
	
	public void setLocale(Locale locale) {
		this.m_locale = locale;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocales()
	 */
	public Enumeration getLocales() {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#isSecure()
	 */
	public boolean isSecure() {
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
	 */
	public RequestDispatcher getRequestDispatcher(String arg0) {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
	 */
	public String getRealPath(String arg0) {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getRemotePort()
	 */
	public int getRemotePort() {
		return m_socket.getPort();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocalName()
	 */
	public String getLocalName() {
		return "localhost";
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocalAddr()
	 */
	public String getLocalAddr() {
		return m_socket.getLocalAddress().toString();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocalPort()
	 */
	public int getLocalPort() {
		return getServerPort();
	}

	public void setMethod(String method) {
		m_method = method;
	}
	
	public void setParameter(String key, String value) {
		if(m_parameterMap.containsKey(key)){
			List<String> vals = m_parameterMap.get(key);
			vals.add(value);
			m_parameterMap.put(key,vals);
		}else{
			List<String> vals = new ArrayList<String>();
			vals.add(value);
			m_parameterMap.put(key,vals);
		}
	}
	
	public void clearParameters() {
		m_parameterMap.clear();
	}
	
	public boolean hasSession() {
		return ((m_session != null) && m_session.isValid());
	}
}
