package eu.wdaqua.qanary.component;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

/**
 * represent the behavior of an annotator following the Qanary methodology
 * 
 * @author AnBo
 *
 */
public abstract class QanaryComponent {

	// TODO need to be changed
	final String questionUrl = "http://localhost:8080/question/28f56d32-b30a-428d-ac90-79372a6f7625/";

	/**
	 * needs to be implemented for any new Qanary component
	 * 
	 * @param myQanaryMessage
	 * @return
	 */
	public abstract QanaryMessage process(QanaryMessage myQanaryMessage);

	/**
	 * fetch raw data for a question
	 * 
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String getQuestionRawData() throws ClientProtocolException, IOException {
		/*
		 * @SuppressWarnings("deprecation") HttpClient client = new
		 * DefaultHttpClient(); HttpGet request = new HttpGet(questionUrl +
		 * QanaryConfiguration.questionRawDataUrlSuffix); HttpResponse response
		 * = client.execute(request);
		 * 
		 * // Get the response BufferedReader rd = new BufferedReader(new
		 * InputStreamReader(response.getEntity().getContent()));
		 * 
		 * String rawText = ""; String line = ""; while ((line = rd.readLine())
		 * != null) { rawText.concat(line); }
		 */
		String rawText = "";
		return rawText;
	}

	/**
	 * get Qanary question
	 */
	public QanaryQuestion getQuestion() {

		// TODO: fetch from endpoint+ingraph via SPARQL the resource of rdf:type
		// qa:Question

		// TODO: create QanaryQuestion object with question URL and raw data
		// this.getQuestionRawData()

		return null;
	}

}
