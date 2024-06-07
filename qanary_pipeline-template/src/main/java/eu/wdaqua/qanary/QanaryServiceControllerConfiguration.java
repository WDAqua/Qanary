package eu.wdaqua.qanary;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryServiceController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class QanaryServiceControllerConfiguration {

    @Bean
    @Primary
    public QanaryServiceController qanaryServiceController(QanaryComponent qanaryComponent) {
        return new QanaryServiceControllerWrapper(qanaryComponent);
    }



}
