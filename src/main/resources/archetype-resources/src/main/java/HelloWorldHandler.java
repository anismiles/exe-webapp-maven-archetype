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
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.beans.factory.annotation.Value;

@Path("/hello")
@Produces({ MediaType.APPLICATION_XML })
public class HelloWorldHandler {

	/** The version. */
	@Value("#{props['version']}")
	String version;

	/** The build. */
	@Value("#{props['build']}")
	String build;

	/** The name. */
	@Value("#{props['name']}")
	String name;

	/** The author. */
	@Value("#{props['author']}")
	String author;

	// time when it all began
	private final long start;

	/** The info. */
	private Info info;

	/**
	 * Instantiates a new home resource.
	 */
	public HelloWorldHandler() {
		start = System.currentTimeMillis();
	}

	/**
	 * Initialize
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@PostConstruct
	public void init() throws IOException {
		info = new Info();
		info.version = version != null ? version : null;
		info.name = name != null ? name : null;
		info.build = build != null ? build : null;
		info.author = author != null ? author : null;
	}

	/**
	 * Responds to "GET /"
	 * 
	 * @return the info
	 */
	@GET
	public Info view() {
		long elapsed = System.currentTimeMillis() - start;
		info.uptime = millisToShortDHMS(elapsed);
		return info;
	}

	/**
	 * Converts time (MS) to Human Readable Format: <dd:>hh:mm:ss
	 * 
	 * @param duration
	 *            the duration
	 * @return the string
	 */
	public static String millisToShortDHMS(long duration) {
		String res = "";

		long days = TimeUnit.MILLISECONDS.toDays(duration);
		long hours = TimeUnit.MILLISECONDS.toHours(duration) - TimeUnit.DAYS.toHours(days);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
				- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
		long seconds = TimeUnit.MILLISECONDS.toSeconds(duration)
				- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));

		if (days == 0) {
			res = String.format("%02d:%02d:%02d", hours, minutes, seconds);
		} else {
			res = String.format("%dd%02d:%02d:%02d", days, hours, minutes, seconds);
		}
		return res;
	}

	/**
	 * class to represent Information.
	 * 
	 * Note: Instance variables are made public knowingly. Saves a hell lot
	 * of time, man!
	 * 
	 * @author animeshkumar
	 * 
	 */
	@XmlRootElement(name = "info")
	public static final class Info {

		/** The name. */
		public String name;

		/** The version. */
		public String version;

		/** The build. */
		public String build;

		/** The author. */
		public String author;

		/** The uptime. */
		public String uptime;
	}
}