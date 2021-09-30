package org.codetracker.experiment.oracle.history;

public class VariableHistoryInfo extends AbstractHistoryInfo {
    private String functionName;
    private String functionKey;
    private int functionStartLine;
    private String variableName;
    private String variableKey;
    private int variableStartLine;


    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionKey() {
        return functionKey;
    }

    public void setFunctionKey(String functionKey) {
        this.functionKey = functionKey;
    }

    public int getFunctionStartLine() {
        return functionStartLine;
    }

    public void setFunctionStartLine(int functionStartLine) {
        this.functionStartLine = functionStartLine;
    }

    public String getVariableKey() {
        return variableKey;
    }

    public void setVariableKey(String variableKey) {
        this.variableKey = variableKey;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public int getVariableStartLine() {
        return variableStartLine;
    }

    public void setVariableStartLine(int variableStartLine) {
        this.variableStartLine = variableStartLine;
    }

    @Override
    public String getElementKey() {
        return variableKey;
    }
}
