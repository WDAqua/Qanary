package eu.wdaqua.qanary.web;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.message.QanaryAvailableQuestions;
import eu.wdaqua.qanary.message.QanaryQuestionCreated;
import eu.wdaqua.qanary.message.QanaryQuestionInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * controller for all service call w.r.t. questions
 *
 * @author AnBo
 */
@Controller
public class QanaryQuestionController {
	private String directoryForStoringQuestionRawData;
	private final String storedQuestionPrefix = "stored-question_";

	private static final Logger logger = LoggerFactory.getLogger(QanaryQuestionController.class);

	// Set this to allow browser requests from other websites
	@ModelAttribute
	public void setVaryResponseHeader(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
	}

	/**
	 * inject QanaryConfigurator
	 */
	@Autowired
	public QanaryQuestionController(final QanaryConfigurator qanaryConfigurator,
			final QanaryPipelineConfiguration myQanaryPipelineConfiguration) {
		this.directoryForStoringQuestionRawData = myQanaryPipelineConfiguration.getQuestionsDirectory();
	}

	/**
	 * synchronous call to start the QA process (POST), return the URL of the
	 * created question
	 */
	@PostMapping(value = "/question", produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> createQuestion(
			@RequestParam(value = QanaryStandardWebParameters.QUESTION, required = true)
			final String questionstring) {

		logger.info("add received question: {}", questionstring);
		QanaryQuestionCreated responseMessage;
		try {
			responseMessage = this.storeQuestion(questionstring);
		} catch (IOException e) {
			// will be caused by problems with file writing
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (URISyntaxException e) {
			// should be caused by a bad URI
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}

		// send a HTTP 201 CREATED message back
		return new ResponseEntity<QanaryQuestionCreated>(responseMessage, HttpStatus.CREATED);
	}

	public QanaryQuestionCreated storeQuestion(String questionstring) throws IOException, URISyntaxException {

		// store the received question in a file
		final String filename = storedQuestionPrefix+"_text_" + UUID.randomUUID().toString();
		final String filepath = Paths.get(this.getDirectoryForStoringQuestionRawData(), filename).toString();

		// check if question directory is actually existing
		if (!Files.isDirectory(Paths.get(this.getDirectoryForStoringQuestionRawData()))) {
			File file = new File(this.getDirectoryForStoringQuestionRawData());
			file.mkdirs();
			logger.warn("created directory for storing questions: {}", this.getDirectoryForStoringQuestionRawData());
		} else {
			logger.warn("directory exists: {}", this.getDirectoryForStoringQuestionRawData());
		}

		// Save the file locally
		try (final BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filepath)))){
			stream.write(questionstring.getBytes(Charset.defaultCharset()));
		}

		final URI uriOfQuestion = new URI(this.getHost() + "/question/" + filename);
		logger.info("uriOfQuestion: {}", uriOfQuestion);

		return new QanaryQuestionCreated(filename, uriOfQuestion);
	}

	/**
	 * synchronous call to start the QA process (POST) with an audio file, return
	 * the URL of the created question
	 */
	@PostMapping(value = "/question_audio", produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> createAudioQuestion(
			@RequestParam(value = QanaryStandardWebParameters.QUESTION, required = true) final MultipartFile file) {

		logger.info("new audio file received: {}", file.getName());
		QanaryQuestionCreated responseMessage;
		try {
			responseMessage = this.storeAudioQuestion(file);
		} catch (IOException e) {
			// will be caused by problems with file writing
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (URISyntaxException e) {
			// should be caused by a bad URI
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}

		// send a HTTP 201 CREATED message back
		return new ResponseEntity<QanaryQuestionCreated>(responseMessage, HttpStatus.CREATED);
	}

	public QanaryQuestionCreated storeAudioQuestion(MultipartFile file) throws IOException, URISyntaxException {

		// store the received question in a file
		final String filename = storedQuestionPrefix+"_audio_" + UUID.randomUUID().toString();
		// check if question directory is actually existing
		if (!Files.isDirectory(Paths.get(this.getDirectoryForStoringQuestionRawData()))) {
			File dir = new File(this.getDirectoryForStoringQuestionRawData());
			dir.mkdirs();
			logger.warn("created directory for storing questions: {}", this.getDirectoryForStoringQuestionRawData());
		} else {
			logger.warn("directory exists: {}", this.getDirectoryForStoringQuestionRawData());
		}

		// Save the file locally
		try {
			if (file.isEmpty()) {
				throw new IOException("Failed to store empty file " + file.getOriginalFilename());
			}
			Files.copy(file.getInputStream(), Paths.get(this.getDirectoryForStoringQuestionRawData(), filename));
		} catch (IOException e) {
			throw new IOException("Failed to store file " + file.getOriginalFilename(), e);
		}

		final URI uriOfQuestion = new URI(this.getHost() + "/question/" + filename);
		logger.info("uriOfQuestion: {}", uriOfQuestion);

		return new QanaryQuestionCreated(filename, uriOfQuestion);
	}

	/**
	 * return directory where the i questions are stored or saved if not existing,
	 * then create the directory
	 */
	private String getDirectoryForStoringQuestionRawData() {
		return this.directoryForStoringQuestionRawData;
	}

	/**
	 * returns the host (scheme and authority) based on the current request
	 *
	 * host and port are not taken from application.properties because
	 * this would not return the correct values for a containerized service
	 */
	private String getHost() {
		try {
			URI uri = getUriOfCurrentReqest();
			return uri.getScheme() + "://" + uri.getAuthority();
		} catch (NullPointerException | URISyntaxException e) {
			logger.error("Current request uri could not be found!");
			logger.debug(e.getMessage());
		}
        assert false;
		return null;
	}

	private URI getUriOfCurrentReqest() throws URISyntaxException {
		HttpServletRequest request = getCurrentHttpServletRequest();
		Assert.notNull(request, "No request was found!");
		return new URI(request.getRequestURL().toString());
	}

	/**
	 * static method to get HttpServletRequest of the current request
	 */
	public static HttpServletRequest getCurrentHttpServletRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes instanceof ServletRequestAttributes) {
			return ((ServletRequestAttributes)requestAttributes).getRequest();
		}
		logger.debug("Method not called in context of HTTP request!");
		return null;
	}

	/**
	 * synchronous call to start the QA process (POST), return the URL of the
	 * created question
	 */
	@DeleteMapping(value = "/question/{questionid}", produces = "application/json")
	@ResponseBody
	public String deleteQuestion(@PathVariable final String questionid) throws HttpRequestMethodNotSupportedException {
		// TODO: please implement deletion of file if and only if they are in
		// this question directory (security!)
		throw new HttpRequestMethodNotSupportedException("not yet implemented");
	}

	/**
	 * return links to all questions
	 */
	@GetMapping(value = "/question/")
	@ResponseBody
	public QanaryAvailableQuestions getQuestions() throws IOException {

		final QanaryAvailableQuestions questions = new QanaryAvailableQuestions(
				this.getDirectoryForStoringQuestionRawData(), this.getHost());

		logger.debug("Number of available questions in '{}': {}", this.getDirectoryForStoringQuestionRawData(),
				questions.getAvailableQuestions().size());

		return questions;
	}

	/**
	 * return links to all information of one given question
	 */
	@GetMapping(value = "/question/{questionid}")
	@ResponseBody
	public QanaryQuestionInformation getQuestion(@PathVariable final String questionid) throws MalformedURLException {
		return new QanaryQuestionInformation(questionid, this.getHost());
	}

	/**
	 * question for a given id, raw return of the data
	 */
	@GetMapping(value = "/question/{questionid}/raw", produces = "text/plain;charset=UTF-8")
	@ResponseBody
	public FileSystemResource getQuestionRawData(@PathVariable final String questionid) {

		// TODO: move outside of this class
		final String filename = Paths.get(this.getDirectoryForStoringQuestionRawData(), questionid).toString();
		return new FileSystemResource(filename);
	}

	/**
	 * fetch the processing status of a given question
	 */
	@GetMapping(value = "/question/{question}/status", headers = "Accept=application/json", produces = {
			"application/json;charset=UTF-8" })
	@ResponseBody
	public String getStatusOfQuestion(@PathVariable final URL questionuri) {
		// TODO: fetch the processing status of the given question URL
		// TODO: OR return the complete RDF object
		return null;
	}

}
