package eu.wdaqua.qanary.web;

import com.hp.hpl.jena.tdb.base.StorageException;

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
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
import org.springframework.web.multipart.MultipartFile;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.message.QanaryAvailableQuestions;
import eu.wdaqua.qanary.message.QanaryQuestionCreated;
import eu.wdaqua.qanary.message.QanaryQuestionInformation;

/**
 * controller for all service call w.r.t. questions
 *
 * @author AnBo
 */
@Controller

public class QanaryQuestionController {
	@Value("${server.host}")
	private String host;
	@Value("${server.port}")
	private String port;
	
    private static final Logger logger = LoggerFactory.getLogger(QanaryQuestionController.class);

    private final QanaryConfigurator qanaryConfigurator;

    // TODO: define directory in config
    private final String directoryForStoringQuestionRawData = "/tmp/questions";

    /**
     * inject QanaryConfigurator
     */
    @Autowired
    public QanaryQuestionController(final QanaryConfigurator qanaryConfigurator) {
        this.qanaryConfigurator = qanaryConfigurator;
    }

    /**
     * synchronous call to start the QA process (POST), return the URL of the created question
     */
    @RequestMapping(value = "/question", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> createQuestion(
            @RequestParam(value = "question", required = true) final String questionstring) {

        logger.info("add received question: " + questionstring);
        // URI uriOfQuestion;
        QanaryQuestionCreated responseMessage;
        try {
            // uriOfQuestion = this.storeQuestion(questionstring);
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

        // final QanaryQuestionCreated responseMessage = new
        // QanaryQuestionCreated(uriOfQuestion);

        // send a HTTP 201 CREATED message back
        return new ResponseEntity<QanaryQuestionCreated>(responseMessage, HttpStatus.CREATED);
    }

    public QanaryQuestionCreated storeQuestion(String questionstring) throws IOException, URISyntaxException {

        // store the received question in a file
        final String filename = UUID.randomUUID().toString();
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
        final BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filepath)));
        stream.write(questionstring.getBytes(Charset.defaultCharset()));
        stream.close();

        final URI uriOfQuestion = new URI(this.getHost() + "/question/" + filename);
        logger.info("uriOfQuestion: " + uriOfQuestion);

        return new QanaryQuestionCreated(filename, uriOfQuestion);
    }

    /**
     * synchronous call to start the QA process (POST) with an audio file, return the URL of the created question
     */
    @RequestMapping(value = "/question_audio", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> createAudioQuestion(
            @RequestParam(value = "question", required = true) final MultipartFile file) {

        logger.info("new audio file recived: " + file.getName());
        // URI uriOfQuestion;
        QanaryQuestionCreated responseMessage;
        try {
            // uriOfQuestion = this.storeQuestion(questionstring);
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

        // final QanaryQuestionCreated responseMessage = new
        // QanaryQuestionCreated(uriOfQuestion);

        // send a HTTP 201 CREATED message back
        return new ResponseEntity<QanaryQuestionCreated>(responseMessage, HttpStatus.CREATED);
    }

    public QanaryQuestionCreated storeAudioQuestion(MultipartFile file) throws IOException, URISyntaxException {

        // store the received question in a file
        final String filename = UUID.randomUUID().toString();
        final String filepath = Paths.get(this.getDirectoryForStoringQuestionRawData(), filename).toString();

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
                throw new StorageException("Failed to store empty file " + file.getOriginalFilename());
            }
            Files.copy(file.getInputStream(), Paths.get(this.getDirectoryForStoringQuestionRawData(), filename));
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + file.getOriginalFilename(), e);
        }

        final URI uriOfQuestion = new URI(this.getHost() + "/question/" + filename);
        logger.info("uriOfQuestion: " + uriOfQuestion);

        return new QanaryQuestionCreated(filename, uriOfQuestion);
    }

    /**
     * return directory where the i questions are stored or saved if not existing, then create the
     * directory
     */
    private String getDirectoryForStoringQuestionRawData() {
        return this.directoryForStoringQuestionRawData;
    }

    /**
     * returns the current name of the host
     */
    private String getHost() {
        // @TODO: replace by configuration
    	
        return host+":"+port;
    }

    /**
     * synchronous call to start the QA process (POST), return the URL of the created question
     */
    @RequestMapping(value = "/question/{questionid}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public String deleteQuestion(@PathVariable final String questionid) throws HttpRequestMethodNotSupportedException {
        // TODO: please implement deletion of file if and only if they are in
        // this question directory (security!)
        throw new HttpRequestMethodNotSupportedException("not yet implemented");
    }

    /**
     * return links to all questions
     */
    @RequestMapping(value = "/question/", method = RequestMethod.GET)
    @ResponseBody
    public QanaryAvailableQuestions getQuestions() throws IOException {

        final QanaryAvailableQuestions questions = new QanaryAvailableQuestions(
                this.getDirectoryForStoringQuestionRawData(), this.getHost());

        logger.debug("Number of available questions in {}: {}", this.getDirectoryForStoringQuestionRawData(),
                questions.getAvailableQuestions().size());

        return questions;
    }

    /**
     * return links to all information of one given question
     */
    @RequestMapping(value = "/question/{questionid}", method = RequestMethod.GET)
    @ResponseBody
    public QanaryQuestionInformation getQuestion(@PathVariable final String questionid) throws MalformedURLException {
        return new QanaryQuestionInformation(questionid, this.getHost());
    }

    /**
     * question for a given id, raw return of the data
     */
    @RequestMapping(value = "/question/{questionid}/raw", method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
    @ResponseBody
    public FileSystemResource getQuestionRawData(@PathVariable final String questionid) throws IOException {

        // TODO: move outside of this class
        final String filename = Paths.get(this.getDirectoryForStoringQuestionRawData(), questionid).toString();
        return new FileSystemResource(filename); 
    }

    /**
     * fetch the processing status of a given question
     */
    @RequestMapping(value = "/question/{question}/status", headers = "Accept=application/json", method = RequestMethod.GET, produces = {
            "application/json;charset=UTF-8"})
    @ResponseBody
    public String getStatusOfQuestion(@PathVariable final URL questionuri) {
        // TODO: fetch the processing status of the given question URL
        // TODO: OR return the complete RDF object
        return null;
    }

}
