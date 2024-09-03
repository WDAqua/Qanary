package eu.wdaqua.qanary.explainability.annotations;

import java.util.List;

public class MethodObject {
    private String className;
    private String methodName;
    private Object[] input;
    private Object output;
    private String outputType;
    private List<String> inputTypes;

    public List<String> getInputTypes() {
        return inputTypes;
    }

    public void setInputTypes(List<String> inputTypes) {
        this.inputTypes = inputTypes;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public Object getOutput() {
        return output;
    }

    public Object[] getInput() {
        return input;// Is this enough?
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setInput(Object[] input) {
        this.input = input;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setOutput(Object output) {
        this.output = output;
    }
}
