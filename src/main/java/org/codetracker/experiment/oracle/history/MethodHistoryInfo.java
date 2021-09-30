package org.codetracker.experiment.oracle.history;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MethodHistoryInfo extends AbstractHistoryInfo {
    private String functionName;
    private String functionKey;
    private int functionStartLine;

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

    @Override
    @JsonIgnore
    public String getElementKey() {
        return functionKey;
    }
}
