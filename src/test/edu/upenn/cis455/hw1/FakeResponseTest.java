package test.edu.upenn.cis455.hw1;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

import edu.upenn.cis455.webserver.*;
import junit.framework.TestCase;

public class FakeResponseTest extends TestCase {
	FakeResponse response;
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
		response = new FakeResponse(request,null,System.out);
	}

	public void testSetCharacterEncoding() {
		assertEquals(response.getCharacterEncoding(), "ISO-8859-1");
		response.setCharacterEncoding("UTF-8");
		assertEquals(response.getCharacterEncoding(), "UTF-8");
	}

	public void testSetContentType() {
		assertEquals(response.getContentType(), "text/html");
		response.setContentType("text/plain");
		assertEquals(response.getContentType(), "text/plain");
	}
	
	
}
