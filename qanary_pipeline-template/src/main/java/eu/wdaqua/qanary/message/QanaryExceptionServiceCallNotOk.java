package eu.wdaqua.qanary.message;

import org.springframework.http.HttpStatus;

/**
 * represents the message of components that was callable but does not return a valid response code
 *
 * TODO: move class to exception package
 *
 * @author AnBo
 */
public class QanaryExceptionServiceCallNotOk extends Exception {

    private static final long serialVersionUID = 7935029187153605509L;

    public QanaryExceptionServiceCallNotOk(String componentName, HttpStatus myHttpStatus) {
        super("Call to service " + componentName + " was not performed correctly. Returns " + myHttpStatus.name() + "/"
                + myHttpStatus.value());
    }

}
