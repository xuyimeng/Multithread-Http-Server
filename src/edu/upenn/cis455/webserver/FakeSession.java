package edu.upenn.cis455.webserver;

import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * @author Todd J. Green
 */
public class FakeSession implements HttpSession {
	
	private boolean m_valid;
	private boolean m_new;
	private String m_sessionId;
	private Map<String, Object> m_attrib;
	private long m_creationTime;
	private long m_lastAccessTime;
	private int m_maxInterval = 60;
	private ServletContext m_context;

	public FakeSession(FakeContext context){
		this.m_sessionId = UUID.randomUUID().toString();
		this.m_valid = true;
		this.m_new = true;
		this.m_attrib = new HashMap<String,Object>();
		this.m_creationTime = System.currentTimeMillis();
		this.m_lastAccessTime = m_creationTime;
		this.m_context = context;
		if(m_context.getAttribute("session-timeout") != null){
			setMaxInactiveInterval(Integer.parseInt(m_context.getAttribute("session-timeout").toString()));
		}
	}
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getCreationTime()
	 */
	public long getCreationTime() {
		return m_creationTime;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getId()
	 */
	public String getId() {
		return m_sessionId;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getLastAccessedTime()
	 */
	public long getLastAccessedTime() {
		return m_lastAccessTime;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getServletContext()
	 */
	public ServletContext getServletContext() {
		return m_context;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
	 */
	public void setMaxInactiveInterval(int arg0) {
		m_maxInterval = arg0;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
	 */
	public int getMaxInactiveInterval() {
		return m_maxInterval;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getSessionContext()
	 */
	public HttpSessionContext getSessionContext() {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String arg0) {
		return m_attrib.get(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
	 */
	public Object getValue(String arg0) {
		return m_attrib.get(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getAttributeNames()
	 */
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(m_attrib.keySet());
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getValueNames()
	 */
	public String[] getValueNames() { 
		return m_attrib.keySet().toArray(new String[m_attrib.size()]);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String arg0, Object arg1) {
		m_attrib.put(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
	 */
	public void putValue(String arg0, Object arg1) {
		m_attrib.put(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String arg0) {
		m_attrib.remove(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
	 */
	public void removeValue(String arg0) {
		m_attrib.remove(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#invalidate()
	 */
	public void invalidate() {
		if(m_valid == false){//method is called on an already invalidated session
			throw new IllegalStateException();
		}
		m_valid = false;//Invalidates this session then unbinds any objects bound to it.
//		HttpServer.sessionMap.remove(getId());
		m_attrib.clear();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#isNew()
	 */
	public boolean isNew() {
		if(m_valid == false){
			throw new IllegalStateException();
		}
		return m_new;
	}

	public boolean isValid() {
		if(!m_valid) return false;
		if((new Date().getTime() - getLastAccessedTime()) > 
		TimeUnit.SECONDS.toMillis(getMaxInactiveInterval())){
			m_valid = false;
		}
		return m_valid;
	}
	public void setLastAccessedTime(long time) {
		m_lastAccessTime = time;
	}
}
