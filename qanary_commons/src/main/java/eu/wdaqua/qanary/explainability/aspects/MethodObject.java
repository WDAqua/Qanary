package eu.wdaqua.qanary.explainability.aspects;

import java.util.Arrays;

public class MethodObject {

    //    private String uuid;
    private String caller;
    private String method;
    private String explanationType; // enum?
    private String explanationValue;
    private Object[] input;
    private Object output;
    private String annotatedBy;
    private boolean errorOccurred;

    public MethodObject(String caller, String method, Object[] input) {
        this.caller = caller;
        this.method = method;
        this.input = input;
    }

    public boolean isErrorOccurred() {
        return errorOccurred;
    }

    public void setErrorOccurred(boolean errorOccurred) {
        this.errorOccurred = errorOccurred;
    }

    public Object[] getInput() {
        return input;
    }

    public void setInput(Object[] input) {
        this.input = input;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    public String getAnnotatedBy() {
        return annotatedBy;
    }

    public void setAnnotatedBy(String annotatedBy) {
        this.annotatedBy = annotatedBy;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getExplanationType() {
        return explanationType;
    }

    public void setExplanationType(String explanationType) {
        this.explanationType = explanationType;
    }

    public String getExplanationValue() {
        return explanationValue;
    }

    public void setExplanationValue(String explanationValue) {
        this.explanationValue = explanationValue;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "MethodObject{" +
                "caller=" + caller +
                ", method='" + method + '\'' +
                ", explanationType='" + explanationType + '\'' +
                ", explanationValue='" + explanationValue + '\'' +
                ", input=" + Arrays.toString(input) +
                ", output=" + output +
                ", annotatedBy='" + annotatedBy + '\'' +
                '}';
    }
}
