package eu.wdaqua.qanary.message;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * represents the message object returned while retrieving all available questions
 *
 * @author AnBo
 */
public class QanaryAvailableQuestions {

    private final List<URL> availablequestions = new LinkedList<>();

    /**
     * The serialization runtime associates with each serializable class a version number, called a
     * serialVersionUID, which is used during deserialization to verify that the sender and receiver
     * of a serialized object have loaded classes for that object that are compatible with respect
     * to serialization
     */
    private static final long serialVersionUID = 42L;

    private static final Logger logger = LoggerFactory.getLogger(QanaryAvailableQuestions.class);

    /**
     * create list of available questions in given directory
     */
    public QanaryAvailableQuestions(String directoryForStoringQuestionRawData, String host) throws IOException {
        File folder = new File(directoryForStoringQuestionRawData);

        if (!folder.isDirectory()) {
            throw new IOException(directoryForStoringQuestionRawData
                    + " is not accessible. Create it? Or change path in config file.");
        }

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                // skip
            } else {
                try {
                    availablequestions.add(new URL(host + "/question/" + fileEntry.getName() + "/"));
                } catch (MalformedURLException e) {
                    logger.warn("could not create URL from " + fileEntry + ": " + e.getMessage());
                    throw e;
                }
            }
        }

        logger.info("found: " + this.availablequestions.size() + " in " + directoryForStoringQuestionRawData);

    }

    public List<URL> getAvailableQuestions() {
        return this.availablequestions;
    }

}
