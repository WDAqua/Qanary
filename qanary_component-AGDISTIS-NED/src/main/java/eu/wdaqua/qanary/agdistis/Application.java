package eu.wdaqua.qanary.agdistis;

import eu.wdaqua.qanary.component.QanaryComponent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
public class Application {


    @Bean
    public QanaryComponent qanaryComponent() {
        return new Agdistis();
    }

    /**
     * default main, can be removed later
     */
    public static void main(String[] args) {
        //Properties p = new Properties();
        //new SpringApplicationBuilder(Application.class).properties(p).run(args);
        SpringApplication.run(Application.class, args);
    }
}
