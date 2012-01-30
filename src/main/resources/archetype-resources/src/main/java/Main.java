/*
 * Copyright 2012 Animesh Kumar <animesh@strumsoft.com>
 * Copyright 2012 Strumsoft http://www.strumsoft.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.ProtectionDomain;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Main class. This will bootstrap and handle Jetty
 * 
 * @author Animesh Kumar
 */
public class Main {

	/** The host. */
	private final String host;

	/** The port. */
	private final int port;

	/** The context path. */
	private final String contextPath;

	/** The work dir path. */
	private final String workDirPath;

	/** The secret. */
	private final String secret;

	/** The server. */
	private Server server;

	/** The context. */
	private WebAppContext context;

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 * @throws Exception
	 *             the exception
	 */
	public static void main(String[] args) throws Exception {
		String command = (args.length > 0) ? args[0] : "usage";
		// stop server
		if ("stop".equals(command)) {
			new Main().stop();
		}
		// start server
		else if ("start".equals(command)) {
			new Main().start();
		}
		// display usage
		else {
			System.out.println("Usage: java -jar <file.jar> [start|stop]\n\t" + "start    Start the server\n\t"
					+ "stop     Stop the server gracefully\n");
			System.exit(-1);
		}
	}

	/**
	 * Instantiates a new main.
	 */
	public Main() {
		try {
			System.out.println("Configuring Jetty embedded server...");
			// Read jetty.properties
			String configFile = System.getProperty("config", "classpath:jetty.properties");
			// if in classpath
			if (configFile.startsWith("classpath:")) {
				configFile = configFile.substring(10);
				System.out.println("Loading from classpath: " + configFile);
				System.getProperties().load(this.getClass().getClassLoader().getResourceAsStream(configFile));
			}
			// else, Load as a normal file
			else {
				System.out.println("Loading from file: " + configFile);
				System.getProperties().load(new FileInputStream(configFile));
			}
		} catch (Exception exp) {
			exp.printStackTrace();
		}

		// populate instance variables
		host = System.getProperty("jetty.host", "localhost");
		port = Integer.parseInt(System.getProperty("jetty.port", "8080"));
		contextPath = System.getProperty("jetty.contextPath", "/");
		workDirPath = System.getProperty("jetty.workDir", null);
		secret = System.getProperty("jetty.secret", "8dfe412e-1db2-4a22-babc-cc83dd8f3883");
	}

	public void start() {
		// start in a new thread
		new Thread(new Runnable() {
			@Override
			public void run() {
				_start();
			}
		}).start();
	}

	/**
	 * Start.
	 */
	private void _start() {
		try {
			// Get the war-file
			ProtectionDomain protectionDomain = Main.class.getProtectionDomain();
			String warFile = protectionDomain.getCodeSource().getLocation().toExternalForm();
			File warDir = new File(protectionDomain.getCodeSource().getLocation().getPath());
			String currentDir = warDir.getParent();

			// Work/Static directory
			File workDir = getWorkDirectory(currentDir, warDir.getName());

			System.out.println(String.format(
					"Jetty Properties ===> \nhost=%s \nport=%s \ncontext=%s \n*secret=%s \nwork-directory=%s \n", host,
					port, contextPath, secret, workDir.getAbsolutePath()));

			// Start a Jetty server
			server = new Server();
			server.setStopAtShutdown(true);

			// Allow 5 seconds to complete.
			server.setGracefulShutdown(5000);

			// Increase thread pool
			QueuedThreadPool threadPool = new QueuedThreadPool();
			threadPool.setMaxThreads(100);
			server.setThreadPool(threadPool);

			// Ensure using the non-blocking connector (NIO)
			Connector connector = new SelectChannelConnector();
			connector.setPort(port);
			connector.setMaxIdleTime(30000);
			server.setConnectors(new Connector[] { connector });

			// Add the warFile (this jar)
			context = new WebAppContext(warFile, contextPath) {
				@Override
				protected void doStart() throws Exception {
					super.doStart();
					if (getUnavailableException() != null) {
						throw (Exception) getUnavailableException();
					}
				}
			};

			context.setServer(server);
			context.setTempDirectory(workDir);

			// handle shutdown
			/**
			 * Inspired by Johannes Brodwall ==>
			 * http://johannesbrodwall.com/2010/03/08/why-and-how-to-use-jetty-in-mission-critical-production/
			 */
			AbstractHandler shutdownHandler = new AbstractHandler() {

				public void handle(String target, Request serverRequest, HttpServletRequest request,
						HttpServletResponse response) throws IOException, ServletException {
					if (!target.equals("/shutdown") || !request.getMethod().equals("POST")
							|| !secret.equals(request.getParameter("secret"))) {
						return;
					}

					response.setContentType("text/plain");
					response.setStatus(SC_OK);
					// call shutdown
					_stop();
					System.exit(0);
				}
			};

			HandlerList handlers = new HandlerList();
			handlers.setHandlers(new Handler[] { shutdownHandler, // shutdown
					context // Handle application
			});
			server.setHandler(handlers);

			// Add lifecycle listener
			server.addLifeCycleListener(new LifeCycle.Listener() {

				@Override
				public void lifeCycleFailure(LifeCycle lc, Throwable th) {
					System.err.println("====> LifeCycle Failure => " + th.getMessage());
				}

				@Override
				public void lifeCycleStarted(LifeCycle lc) {
					System.out.println(String.format("====> Started jetty @ http://%s:%d/", host, port));
				}

				@Override
				public void lifeCycleStarting(LifeCycle lc) {
					System.out.println(String.format("====> Starting jetty @ http://%s:%d/ ...", host, port));
				}

				@Override
				public void lifeCycleStopped(LifeCycle lc) {
					System.out.println(String.format("====> Stopped jetty @ http://%s:%d/", host, port));
				}

				@Override
				public void lifeCycleStopping(LifeCycle lc) {
					System.out.println(String.format("====> Stopping jetty @ http://%s:%d/ ...", host, port));
				}
			});
			// start and join
			server.start();
			server.join();
		} catch (Exception e) {
			System.err.println("====> Error => " + e.getMessage());
			_stop();
		}
	}

	/**
	 * Stop.
	 * 
	 * @throws URISyntaxException
	 *             the uRI syntax exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void stop() throws URISyntaxException, IOException {
		try {
			System.out.println(String.format("====> Attempting to stop jetty @ http://%s:%d/", host, port));

			// Build parameter string
			String _url = "http://" + host + ":" + port + "/shutdown";
			String data = "secret=" + secret;
			URL url = new URL(_url);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
			out.write(data);
			out.flush();
			System.out.println("Response ==> " + conn.getResponseCode() + " " + conn.getResponseMessage());
			out.close();
		} catch (Exception ignored) {
		}
	}

	/**
	 * Stop.
	 * 
	 * @param server
	 *            the server
	 */
	private void _stop() {
		try {
			if (null != server) {
				server.stop();
			}
			if (null != context) {
				context.stop();
			}
		} catch (Exception e) {
			System.err.println("==> Error: " + e.getMessage());
		}
	}

	/**
	 * Gets the temp directory.
	 * 
	 * @param currentDir
	 *            the current dir
	 * @param folder
	 *            the folder
	 * @return the temp directory
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private File getWorkDirectory(String currentDir, String folder) throws IOException {
		File workDir;
		if (workDirPath != null) {
			workDir = new File(workDirPath);
		} else {
			workDir = new File(currentDir, folder + ".work");
		}
		// FileUtils.deleteDirectory(workDir);
		workDir.delete();
		workDir.mkdirs();
		return workDir;
	}
}