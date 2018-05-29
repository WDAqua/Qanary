package eu.wdaqua.qanary.business;

import de.codecentric.boot.admin.model.Application;

/**
 * Created by didier on 27.03.16.
 */
public class QanaryComponent {
    private final Application application;
    private boolean used;

    private QanaryComponent(Application application) {
        this.application = application;
    }

    public QanaryComponent(Application application, boolean used) {
        this(application);
        this.used = used;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public String getName() {
        return application.getName();
    }

    public String getUrl() {
        return application.getServiceUrl();
    }
}
