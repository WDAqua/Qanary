package eu.wdaqua.qanary.languagedetection;

import java.net.MalformedURLException;
import java.net.URL;

import eu.wdaqua.qanary.component.QanaryQuestion;
import eu.wdaqua.qanary.component.QanaryUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.Detector;

@Component
public class LanguageDetection extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(LanguageDetection.class);

	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        long startTime = System.currentTimeMillis();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
        logger.info("Qanary Message: {}", myQanaryMessage);

        //STEP1: Retrieve the question
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

        // question string is required as input for the service call
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        logger.info("Question: {}", myQuestion);

		//STEP2: The question is send to the language recognition library
        Detector detector = DetectorFactory.create();
        detector.append(myQuestion);
        String lang = detector.detect();
        logger.info("Language: {}", lang);

        //The language tags are already aligned with http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry

        //STEP 3: The language tag is pushed to the triple store
        try {
			logger.info("store data in graph {}", myQanaryMessage.getValues().get(new URL(QanaryMessage.endpointKey)));
            String sparql="";
            sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
                    + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
                    + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
                    + "INSERT { "
                    + "GRAPH <" + myQanaryUtils.getInGraph() + "> { "
                    + "  ?a a qa:AnnotationOfQuestionLanguage ; "
                    + "     oa:hasTarget <" + myQanaryQuestion.getUri() + ">; "
                    + "     oa:hasSource \"" + lang +"\" ;"
                    + "     oa:annotatedBy <https://code.google.com/archive/p/language-detection/> ; "
                    + "	    oa:AnnotatedAt ?time . "
                    + "}} "
                        + "WHERE { "
                        + "    BIND (IRI(str(RAND())) AS ?a) ."
                        + "    BIND (now() as ?time) "
                        + "}";
            myQanaryUtils.updateTripleStore(sparql);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return myQanaryMessage;
	}

}
