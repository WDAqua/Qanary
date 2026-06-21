package qald.evaluator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestion;
import eu.wdaqua.qanary.qald.evaluator.qaldreader.TurtleResultWriter;

class TurtleResultWriterTest {

    @Test
    void writesHeaderQuestionAndEntityLines(@TempDir Path tmp) throws IOException {
        Path out = tmp.resolve("result.ttl");

        String json = "{"
                + "\"id\": 5,"
                + "\"question\": [{\"language\": \"en\", \"string\": \"Where is Berlin?\"}],"
                + "\"query\": {\"sparql\": \"PREFIX dbr: <http://dbpedia.org/resource/> "
                + "SELECT ?x WHERE { dbr:Berlin ?p ?x . }\"}"
                + "}";
        QaldQuestion question = new QaldQuestion(new Gson().fromJson(json, JsonObject.class));

        TurtleResultWriter writer = new TurtleResultWriter(out.toString());
        writer.writeQaldQuestionInformation(question);
        writer.writeEntityInQuestion(5, "http://dbpedia.org/resource/Berlin", "expectedResource");
        writer.close();

        String content = Files.readString(out);
        assertTrue(content.contains("# automatically created benchmark result"));
        assertTrue(content.contains("PREFIX qaldevalquestion:"));
        assertTrue(content.contains("qaldevalquestion:5 rdf:label \"Where is Berlin?\"^^xsd:string ."));
        assertTrue(content.contains(
                "qaldevalquestion:5 qaldevaluation:expectedResource <http://dbpedia.org/resource/Berlin> ."));
    }

    @Test
    void constructorSwallowsIoErrorForUnwritableTarget(@TempDir Path tmp) {
        // target path points into a non-existent sub-directory -> FileWriter fails;
        // the constructor must catch the IOException rather than propagate it
        String unwritable = tmp.resolve("does-not-exist").resolve("result.ttl").toString();
        // must not throw
        new TurtleResultWriter(unwritable);
    }
}
