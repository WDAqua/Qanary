package eu.wdaqua.qanary.web;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.PropertySource;

public class ReloadablePropertySource extends PropertySource {

    private final ReloadingFileBasedConfigurationBuilder<PropertiesConfiguration> builder;

    public ReloadablePropertySource(String name, ReloadingFileBasedConfigurationBuilder<PropertiesConfiguration> builder) {
        super(name);
        this.builder = builder;
    }

    public ReloadablePropertySource(String name, String path) {
        super(StringUtils.isEmpty(name) ? path : name);
        this.builder = new ReloadingFileBasedConfigurationBuilder<>(PropertiesConfiguration.class);
        this.builder.configure(new Parameters().properties().setFileName(path));
    }

    @Override
    public Object getProperty(String prop) {
        try {
            // pick up external file changes (replaces FileChangedReloadingStrategy)
            builder.getReloadingController().checkForReloading(null);
            return builder.getConfiguration().getProperty(prop);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
