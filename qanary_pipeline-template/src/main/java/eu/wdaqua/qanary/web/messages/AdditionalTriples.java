package eu.wdaqua.qanary.web.messages;

import java.io.FileOutputStream;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AdditionalTriples {
	//TODO: read from properties
	private String ttlFilePath = "additional-triples.ttl";
	private URI ttlFileUri;

	private static final Logger logger = LoggerFactory.getLogger(AdditionalTriples.class);

	public AdditionalTriples(String triples) {
		if (triples != null) {
			try {
				writeTriplesToFile(triples);
			} catch (Exception e) {
				logger.warn("storing no additional triples: \n{}", e.getMessage());
			}
		} else {
			logger.info("initializing no additional triples");
		}
	}

	public void writeTriplesToFile(String triples) {
		try (FileOutputStream out = new FileOutputStream(this.ttlFilePath)) {
			byte[] bytes = triples.getBytes();
			out.write(bytes);
		} catch (IOException e) {
			//TODO: handle
		}
	}

	public String getFilePath() {
		return this.ttlFilePath;
	}

	public URI getFileUri() throws URISyntaxException {
		return new URI(this.getFilePath());
	}


}
