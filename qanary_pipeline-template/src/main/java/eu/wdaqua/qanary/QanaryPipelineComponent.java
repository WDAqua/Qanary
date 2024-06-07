package eu.wdaqua.qanary;


import eu.wdaqua.qanary.commons.QanaryMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import eu.wdaqua.qanary.component.QanaryComponent;

@Component
@ComponentScan(basePackages = { "eu.wdaqua.qanary" })
public class QanaryPipelineComponent extends QanaryComponent {
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        return null;
    }
}
