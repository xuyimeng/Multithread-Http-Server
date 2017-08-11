package test.edu.upenn.cis455.hw1;
import edu.upenn.cis455.webserver.*;
import junit.framework.TestCase;

public class FakeSessionTest extends TestCase{
	FakeSession session;
	FakeContext context;
	
	protected void setUp() throws Exception {
		context = new FakeContext();
		session = new FakeSession(context);
	}

	public void testSetAttribute() {
		session.setAttribute("test_attr", 123);
		assertEquals(123,session.getAttribute("test_attr"));
	}
	
	public void testRemoveAttribute() {
		session.setAttribute("test_attr", 567);
		assertEquals(567,session.getAttribute("test_attr"));
		session.removeAttribute("test_attr");
		assertEquals(session.getAttribute("test_attr"), null);
	}

	public void testPutValue() {
		Object o = new Object();
		session.putValue("test_value", o);
		assertEquals(session.getValue("test_value"), o);
	}
	
	public void testIsNew() {
		assertTrue(session.isNew());
	}

	public void testInvalidate() {
		assertTrue(session.isValid());
		session.invalidate();
		assertFalse(session.isValid());
	}
}
