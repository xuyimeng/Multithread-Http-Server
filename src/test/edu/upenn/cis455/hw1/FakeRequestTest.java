package test.edu.upenn.cis455.hw1;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

import edu.upenn.cis455.webserver.*;
import junit.framework.TestCase;

public class FakeRequestTest extends TestCase {
	FakeRequest request;
	HttpRequest req;
	FakeContext fc;
	HashMap<String,String> servletUrlMap;

	protected void setUp() throws Exception {
		request = new FakeRequest();
		servletUrlMap = new HashMap<String,String>();
		servletUrlMap.put("/test/*","testServlet");
		FileReader in = new FileReader("files/unit_test.txt");
		BufferedReader br = new BufferedReader(in);
		req = new HttpRequest(br,servletUrlMap);
		fc = new FakeContext();
		HttpServer.sessionMap = new HashMap<String,FakeSession>();
		request = new FakeRequest(null,req,fc);
	}

	public void testGetMethod() {
		assertEquals("POST",request.getMethod());
	}

	public void testGetHeader() {
		assertEquals("localhost:8888",request.getHeader("Host"));
	}

	public void testGetRequestURI() {
		assertEquals("/test/path",request.getRequestURI());
	}

	public void testGetParameterValues() {
		String[] res = request.getParameterValues("test");
		assertEquals("1",res[0]);
	}
	
	public void testGetParameterQueryString() {
		String[] res = request.getParameterValues("num");
		assertEquals("2",res[0]);
	}

	public void testGetCharacterEncoding() {
		assertEquals( "ISO-8859-1",request.getCharacterEncoding());
	}

	public void testGetContentType() {
		assertEquals("text/html",request.getContentType());
	}
	
	public void testGetContentLength() {
		assertEquals(request.getContentLength(), 6);
	}
	
	public void testGetContent() {
		assertEquals("test=1",req.reqContentStr);
	}

	public void testGetProtocol() {
		assertEquals( "HTTP/1.0",request.getProtocol());
	}

}
