package eu.wdaqua.qanary.web;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.env.PropertySource;

public class ReloadablePropertySource extends PropertySource {

    PropertiesConfiguration propertiesConfiguration;

    public ReloadablePropertySource(String name, PropertiesConfiguration propertiesConfiguration) {
        super(name);
        this.propertiesConfiguration = propertiesConfiguration;
    }

    public ReloadablePropertySource(String name, String path) {
        super(StringUtils.isEmpty(name)? path:name);
        try {
            this.propertiesConfiguration = new PropertiesConfiguration(path);
            this.propertiesConfiguration.setReloadingStrategy(new FileChangedReloadingStrategy());
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getProperty(String prop) {
        return propertiesConfiguration.getProperty(prop);
    }
}
