import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.beans.factory.annotation.Value;

public class JettyTestServer {

	@Value("#{testProps['jetty.port']}")
	Integer port;

	@Value("#{testProps['jetty.host']}")
	String host;

	WebAppContext context;

	Server server;

	@PostConstruct
	public void init() throws IllegalArgumentException, IOException {
		try {
			server = new Server();
			Connector conn = new SocketConnector();
			conn.setHost(host);
			conn.setPort(port);
			server.setConnectors(new Connector[] { conn });

			context = new WebAppContext();
			context.setContextPath("/");
			context.setWar("src/main/webapp");
			server.setHandler(context);
			server.start();
			// Note: do NOT join, otherwise current thread will be blocked and no
			// tests can be performed.
			// server.join();
		} catch (Exception ignore) {
			if (server != null) {
				try {
					server.stop();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@PreDestroy
	public void destroy() {
		if (context != null) {
			try {
				context.stop();
			} catch (Exception e) {
			}
		}
		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
			}
		}
	}
}