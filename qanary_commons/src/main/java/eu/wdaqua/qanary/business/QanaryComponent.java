package eu.wdaqua.qanary.business;

/**
 * Created by didier on 27.03.16.
 */
public class QanaryComponent {
    private final String componentName;
    private boolean used;
    private final String serviceUrl;

    public QanaryComponent(String componentName, String serviceUrl, boolean used) {
        this.componentName = componentName;
        this.serviceUrl = serviceUrl;        
        this.used = used;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public String getName() {
        return this.componentName;
    }

    public String getUrl() {
        return this.serviceUrl;
    }
}
