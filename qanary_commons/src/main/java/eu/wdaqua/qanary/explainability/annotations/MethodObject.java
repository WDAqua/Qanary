package eu.wdaqua.qanary.explainability.annotations;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MethodObject {

//    private UUID uuid;
    private UUID caller;
    private String method;
    private String explanationType; // enum?
    private String explanationValue;
    private Object[] input;
    private Object output;
    private String annotatedBy;

    public MethodObject(UUID caller, String method, Object[] input, String annotatedBy) {
        this.caller = caller;
        this.method = method;
        this.input = input;
        this.annotatedBy = annotatedBy;
    }

    public Object[] getInput() {
        return input;
    }

    public Object getOutput() {
        return output;
    }

    public String getAnnotatedBy() {
        return annotatedBy;
    }

    public UUID getCaller() {
        return caller;
    }

    public String getExplanationType() {
        return explanationType;
    }

    public String getExplanationValue() {
        return explanationValue;
    }

    public String getMethod() {
        return method;
    }

    public void setAnnotatedBy(String annotatedBy) {
        this.annotatedBy = annotatedBy;
    }

    public void setCaller(UUID caller) {
        this.caller = caller;
    }

    public void setExplanationType(String explanationType) {
        this.explanationType = explanationType;
    }

    public void setExplanationValue(String explanationValue) {
        this.explanationValue = explanationValue;
    }

    public void setInput(Object[] input) {
        this.input = input;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setOutput(Object output) {
        this.output = output;
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
