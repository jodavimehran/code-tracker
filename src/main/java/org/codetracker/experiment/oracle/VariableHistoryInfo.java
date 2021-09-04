package org.codetracker.experiment.oracle;

import java.util.ArrayList;
import java.util.List;

public class VariableHistoryInfo {
    private String repositoryName;
    private String repositoryWebURL;
    private String filePath;
    private String functionName;
    private String functionKey;
    private int functionStartLine;

    private String variableName;
    private String variableKey;
    private int variableStartLine;

    private String startCommitId;
    private String branchName;
    private final List<ChangeHistory> expectedChanges = new ArrayList<>();

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

    public String getStartCommitId() {
        return startCommitId;
    }

    public void setStartCommitId(String startCommitId) {
        this.startCommitId = startCommitId;
    }

    public List<ChangeHistory> getExpectedChanges() {
        return expectedChanges;
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
