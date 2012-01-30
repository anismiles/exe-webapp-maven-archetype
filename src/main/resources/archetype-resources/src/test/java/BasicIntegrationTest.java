import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring-test.xml", "classpath:spring.xml" })
public class BasicIntegrationTest {

	@Value("#{testProps['jetty.port']}")
	Integer port;

	@Value("#{testProps['jetty.host']}")
	String host;

	private WebResource webResource = null;

	@Before
	public void setUp() throws Exception {
		String base = "http://" + host + ":" + port;
		ClientConfig config = new DefaultClientConfig();
		webResource = Client.create(config).resource(base);
	}

	@After
	public void tearDown() throws Exception {
		webResource = null;
	}

	/**
	 * GET / ==> should execute index.jsp
	 */
	@Test
	public void indexJSP() {
		ClientResponse res = webResource.path("/").get(ClientResponse.class);
		System.out.println(">> " + res.getEntity(String.class));
		Assert.assertEquals(200, res.getStatus());
	}

	/**
	 * GET /rest/hello ==> should execute HelloWorldHandler.view()
	 */
	@Test
	public void helloWorldXML() {
		ClientResponse res = webResource.path("/rest/hello").get(ClientResponse.class);
		System.out.println(">> " + res.getEntity(String.class));
		Assert.assertEquals(200, res.getStatus());
	}

	/**
	 * GET /static/hello.html ==> should return /static/hello.html file
	 */
	@Test
	public void helloHTML() {
		ClientResponse res = webResource.path("/static/hello.html").get(ClientResponse.class);
		System.out.println(">> " + res.getEntity(String.class));
		Assert.assertEquals(200, res.getStatus());
	}

}