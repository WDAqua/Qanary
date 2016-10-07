package eu.wdaqua.qanary.speach;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

@Component
public class SpeachRecognitionKaldi extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(SpeachRecognitionKaldi.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question


		//curl -T test/data/english_test.wav  "http://localhost:8888/client/dynamic/recognize"
		Resource resource = new FileSystemResource("/tmp/questions/be1061d8-a67c-4ae1-807c-eae9dfba5626");
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
		//parts.add("Content-Type", "image/jpeg"); 
		parts.add("file", resource);
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.exchange("http://localhost:8888/client/dynamic/recognize",
				HttpMethod.GET,
				new HttpEntity<MultiValueMap<String, Object>>(parts),
				String.class);


		try {
			logger.info("store data in graph {}", myQanaryMessage.getValues().get(new URL(QanaryMessage.endpointKey)));
			// TODO: insert data in QanaryMessage.outgraph
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)

		return myQanaryMessage;
	}

}
