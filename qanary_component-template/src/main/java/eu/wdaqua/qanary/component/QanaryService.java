package eu.wdaqua.qanary.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

import java.util.Properties;

@SpringBootApplication
@EnableAutoConfiguration
@RestController
@Configuration
@ComponentScan("eu.wdaqua.qanary.component")
public class QanaryService {

    /**
     * default main, can be removed later
     */

    public static void main(String[] args) {
        Properties p = new Properties();
        new SpringApplicationBuilder(QanaryService.class).properties(p).run(args);
        //SpringApplication.run(QanaryService.class, args);
    }
}
