package eu.wdaqua.qanary.qald.evaluator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

/**
 * Application context
 *
 * @author joraojr
 */


@SpringBootApplication
public class Application {

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);
        QaldEvaluatorApplication evaluator = applicationContext.getBean(QaldEvaluatorApplication.class);
        evaluator.process();
    }

}

