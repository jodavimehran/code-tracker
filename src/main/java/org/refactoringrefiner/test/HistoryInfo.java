package org.refactoringrefiner.test;

import java.util.LinkedHashMap;

public class HistoryInfo {
    private String repositoryName;
    private String repositoryWebURL;
    private String filePath;
    private String functionName;
    private String functionKey;
    private int functionStartLine;

    private String variableName;
    private String variableKey;
    private int variableStartLine;

    private String startCommitName;
    private String branchName;
    private LinkedHashMap<String, String> expectedResult = new LinkedHashMap<>();


    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getRepositoryWebURL() {
        return repositoryWebURL;
    }

    public void setRepositoryWebURL(String repositoryWebURL) {
        this.repositoryWebURL = repositoryWebURL;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

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

    public String getStartCommitName() {
        return startCommitName;
    }

    public void setStartCommitName(String startCommitName) {
        this.startCommitName = startCommitName;
    }

    public LinkedHashMap<String, String> getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(LinkedHashMap<String, String> expectedResult) {
        this.expectedResult = expectedResult;
    }

    public int getFunctionStartLine() {
        return functionStartLine;
    }

    public void setFunctionStartLine(int functionStartLine) {
        this.functionStartLine = functionStartLine;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
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
}
