package eu.wdaqua.qanary.qald.evaluator.qaldreader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TurtleResultWriter {
    private static final Logger logger = LoggerFactory.getLogger(TurtleResultWriter.class);

    private BufferedWriter writer;

    public TurtleResultWriter(String filename) {

        logger.debug("write results to file: {}", filename);
        try {
            writer = new BufferedWriter(new FileWriter(filename, false));
            writer.append("# automatically created benchmark result\n");
            writer.append("PREFIX qaldevalquestion: <urn:qaldevalquestion>\n");
            writer.append("PREFIX qaldevaluation: <urn:qaldevaluation>\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeQaldQuestionInformation(QaldQuestion qaldQuestion) {
        String question = qaldQuestion.getQuestion();
        String qaldId = "qaldevalquestion:" + qaldQuestion.getQaldId();

        write(qaldId + " rdf:label \"" + question + "\"^^xsd:string .");

    }

    public void writeEntityInQuestion(int qaldId, String resource, String predicate) {
        String line = "qaldevalquestion:" + qaldId + " qaldevaluation:" + predicate + " <" + resource + "> .";
        write(line);
    }

    private void write(String out) {
        try {
            writer.append(out + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            logger.debug("file closed.");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
