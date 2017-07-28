package eu.wdaqua.qanary.languagedetection;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import eu.wdaqua.qanary.component.QanaryComponent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.util.Properties;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
public class Application {
	private Properties props = new Properties();
	/**
	* this method is needed to make the QanaryComponent in this project known
	* to the QanaryServiceController in the qanary_component-template
	* 
	* @return
	*/
	@Bean
	public QanaryComponent qanaryComponent() {
		try {
			//The my.properties file is created throught the pom.xml file
			java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("config/my.properties");
			props.load(is);
			DetectorFactory.loadProfile(props.getProperty("profile_directory"));
		} catch (LangDetectException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new LanguageDetection();
	}
	
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
