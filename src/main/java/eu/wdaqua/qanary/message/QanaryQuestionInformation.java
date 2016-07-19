package eu.wdaqua.qanary.message;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * represents message send while asking for the information on a particular questions
 *
 * @author AnBo
 */
public class QanaryQuestionInformation {

    public final URL rawdata;
    public String questionID;

    /**
     *
     * @param questionID
     * @throws MalformedURLException
     */
    public QanaryQuestionInformation(String questionID, String host) throws MalformedURLException {
        this.questionID = questionID;
        this.rawdata = new URL(host + "/question/" + questionID + "/raw");
    }

}
