package eu.wdaqua.qanary.web;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.message.QanaryAvailableQuestions;
import eu.wdaqua.qanary.message.QanaryQuestionCreated;
import eu.wdaqua.qanary.message.QanaryQuestionInformation;

/**
 * controller for all service call w.r.t. questions
 * 
 * @author AnBo
 *
 */
@Controller
public class QanaryQuestionController {

	private static final Logger logger = LoggerFactory.getLogger(QanaryQuestionController.class);

	private final QanaryConfigurator qanaryConfigurator;

	// TODO: define directory in config
	String directoryForStoringQuestionRawData = "/tmp/questions";

	/**
	 * inject QanaryConfigurator
	 */
	@Autowired
	public QanaryQuestionController(QanaryConfigurator qanaryConfigurator) {
		this.qanaryConfigurator = qanaryConfigurator;
	}

	/**
	 * returns the current name of the host
	 * 
	 * @return
	 */
	private String getHost() {
		// @TODO: replace by configuration
		return "http://localhost:8080";
	}

	/**
	 * return directory where the raw questions are stored or saved if not
	 * existing, then create the directory
	 * 
	 * @return
	 */
	private String getDirectoryForStoringQuestionRawData() {
		return this.directoryForStoringQuestionRawData;
	}

	/**
	 * synchronous call to start the QA process (POST), return the URL of the
	 * created question
	 * 
	 * @param questionstring
	 */
	@RequestMapping(value = "/question", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> createQuestion(@RequestParam(value = "question", required = true) String questionstring) {

		logger.info("add received question: " + questionstring);

		// store the received question in a file
		String filename = UUID.randomUUID().toString();
		String filepath = Paths.get(this.getDirectoryForStoringQuestionRawData(), filename).toString();

		// Save the file locally
		try {
			BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filepath)));
			stream.write(questionstring.getBytes(Charset.defaultCharset()));
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}

		// TODO: insert the question into triplestore

		// TODO: execute? the QA process: for each of your components call
		// /annotatequestion by passing a QanaryMessage to them, OPEN ISSUE:
		// execute the alignment on business or component side?

		QanaryQuestionCreated responseMessage;
		try {
			URI uriOfQuestion = new URI(this.getHost() + "/question/" + filename);
			logger.info("uriOfQuestion: " + uriOfQuestion);
			responseMessage = new QanaryQuestionCreated(uriOfQuestion);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// send a HTTP 201 CREATED message back
		return new ResponseEntity<QanaryQuestionCreated>(responseMessage, HttpStatus.CREATED);
	}

	/**
	 * synchronous call to start the QA process (POST), return the URL of the
	 * created question
	 * 
	 * @param questionstring
	 * @throws HttpRequestMethodNotSupportedException
	 */
	@RequestMapping(value = "/question/{questionid}", method = RequestMethod.DELETE, produces = "application/json")
	@ResponseBody
	public String deleteQuestion(@PathVariable String questionid) throws HttpRequestMethodNotSupportedException {
		// TODO: please implement deletion of file if and only if they are in
		// this question directory (security!)
		throw new HttpRequestMethodNotSupportedException("not yet implemented");
	}

	/**
	 * return links to all questions
	 * 
	 * @param questionstring
	 * @throws IOException
	 */
	@RequestMapping(value = "/question/", method = RequestMethod.GET)
	@ResponseBody
	public QanaryAvailableQuestions getQuestions() throws IOException {

		QanaryAvailableQuestions questions = new QanaryAvailableQuestions(this.getDirectoryForStoringQuestionRawData(),
				this.getHost());

		logger.debug("Number of available questions in {}: {}", this.getDirectoryForStoringQuestionRawData(),
				questions.getAvailableQuestions().size());

		return questions;
	}

	/**
	 * return links to all information of one given question
	 * 
	 * @param questionstring
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	@RequestMapping(value = "/question/{questionid}", method = RequestMethod.GET)
	@ResponseBody
	public QanaryQuestionInformation getQuestion(@PathVariable String questionid) throws MalformedURLException {
		return new QanaryQuestionInformation(questionid, this.getHost());
	}

	/**
	 * question for a given id, raw return of the data
	 * 
	 * @param questionstring
	 */
	@RequestMapping(value = "/question/{questionid}/raw", method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
	@ResponseBody
	public String getQuestionRawData(@PathVariable String questionid) {

		// TODO: move outside of this class
		String filename = Paths.get(this.getDirectoryForStoringQuestionRawData(), questionid).toString();
		String content = null;
		File file = new File(filename);
		FileReader reader = null;
		try {
			reader = new FileReader(file);
			char[] chars = new char[(int) file.length()];
			reader.read(chars);
			content = new String(chars);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return content;
	}

	/**
	 * fetch the processing status of a given question
	 * 
	 * @param questionstring
	 */
	@RequestMapping(value = "/question/{question}/status", headers = "Accept=application/json", method = RequestMethod.GET, produces = {
			"application/json;charset=UTF-8" })
	@ResponseBody
	public String getStatusOfQuestion(@PathVariable URL questionuri) {
		// TODO: fetch the processing status of the given question URL
		// TODO: OR return the complete RDF object
		return null;
	}

}
