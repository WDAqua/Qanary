package eu.wdaqua.qanary.web.messages;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.io.File;
import java.io.BufferedOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;


@Component
@Scope(value = "singelton")
public class AdditionalTriples {
	private Environment env; 
	private String id;
	private String filePath;
	private String triples;

	private static final Logger logger = LoggerFactory.getLogger(AdditionalTriples.class);

	public AdditionalTriples(String triples, Environment environment) {
		this.env = environment;
		String ttlFileDirectory = env.getProperty("qanary.process.additional-triples-directory");
		if (triples != null && !triples.isBlank()) {
			this.triples = triples;
			try {
				writeTriplesToFile(triples, ttlFileDirectory);
				logger.info("stored additional triples as {}", this.getStringFilePath());
			} catch (Exception e) {
				logger.warn("storing no additional triples: \n{}", e.getMessage());
			}
		} else {
			logger.info("initializing no additional triples");
		}
	}

	private void writeTriplesToFile(String triples, String ttlFileDirectory) throws IOException {
		id = UUID.randomUUID().toString();
		String fileName = id+".ttl"; // TODO: add a prefix
		filePath = Paths.get(ttlFileDirectory, fileName).toString();

		if(!Files.isDirectory(Paths.get(ttlFileDirectory))) {
			File file = new File(ttlFileDirectory);
			file.mkdirs();
			logger.warn("created directory for storing additional triples: {}", ttlFileDirectory);
		} else {
			logger.warn("directory already exists: {}", ttlFileDirectory);
		}

		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
		out.write(triples.getBytes(Charset.defaultCharset()));
		out.close();
	}

	public String getUUIDString() {
		return this.id;
	}

	public String getStringFilePath() {
		return filePath;
	}

	public URI getUriFilePath() throws URISyntaxException {
		return new URI(filePath);
	}

	public String getTriples() {
		return this.triples;
	}
}
