package eu.wdaqua.qanary.proxy;

import de.codecentric.boot.admin.config.*;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Created by didier cherix on 03.04.16.
 */
public class AdminServerWithProxyImportSelector implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{MailNotifierConfiguration.class.getCanonicalName(),
                HazelcastStoreConfiguration.class.getCanonicalName(),
                AdminServerWebConfiguration.class.getCanonicalName(),
                DiscoveryClientConfiguration.class.getCanonicalName(),
                ExtReverseZuulProxyConfiguration.class.getCanonicalName(),
                PagerdutyNotifierConfiguration.class.getCanonicalName()};
    }
}
