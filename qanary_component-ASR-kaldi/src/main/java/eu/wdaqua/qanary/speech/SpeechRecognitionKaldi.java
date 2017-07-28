package eu.wdaqua.qanary.speech;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

@Component
public class SpeechRecognitionKaldi extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionKaldi.class);
	@Value("${spring.boot.admin.url}")
	private String qanaryHost;
	@Value("${kaldi.url}")
        private String kaldiHost;

	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

		// STEP1: Retrieve information that are needed for the computations

		// the class QanaryUtils provides some helpers for standard tasks
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage);

		byte[] binary = myQanaryQuestion.getAudioRepresentation();
		logger.info("process: {}", myQanaryMessage);

		//Command line command: curl -T test/data/english_test.wav  "http://localhost:8080/client/dynamic/recognize"
		logger.info("Kaldi service called: {}",kaldiHost);
		HttpPost post = new HttpPost(kaldiHost);

		post.setEntity(new ByteArrayEntity(binary));
		HttpClient client = HttpClientBuilder.create().build();
		HttpResponse response = client.execute(post);
		ResponseHandler<String> handler = new BasicResponseHandler();
		String body = handler.handleResponse(response);
		logger.info("Result ASR service: {}", body);
		JSONObject obj = new JSONObject(body);
		JSONArray array = obj.getJSONArray("hypotheses");
		String textTranslation = ((JSONObject)array.get(0)).getString("utterance");
		logger.info("Translation: {}", textTranslation);


		//Resource resource = new FileSystemResource("/tmp/questions/be1061d8-a67c-4ae1-807c-eae9dfba5626");
		//MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
		//parts.add("Content-Type", "image/jpeg");
		//parts.add("file", resource);
		//RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
		//List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
		//interceptors.add(new LoggingRequestInterceptor());
		//restTemplate.setInterceptors(interceptors);
		//ResponseEntity<String> result = restTemplate.exchange("http://localhost:8888/client/dynamic/recognize",
		//		HttpMethod.GET,
		//		new HttpEntity<MultiValueMap<String, Object>>(parts),
		//		String.class);

		//Create a uri where the question is exposed, use the pi
		//curl  -F "question=My new question" localhost:8080/question


		myQanaryQuestion.putTextRepresentation(textTranslation);

		logger.info("apply commons alignment on outgraph");
		return myQanaryMessage;
	}
}
