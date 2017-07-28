package eu.wdaqua.qanary.languagedetection;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.Detector;

@Component
public class LanguageDetection extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(LanguageDetection.class);

	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        long startTime = System.currentTimeMillis();
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
        logger.info("store data in graph {}", myQanaryMessage.getEndpoint());
        myQanaryQuestion.setLanguageText(lang);
        //        + "     oa:annotatedBy <https://code.google.com/archive/p/language-detection/> ; "
		return myQanaryMessage;
	}

}
