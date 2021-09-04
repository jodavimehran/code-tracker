package org.codetracker.experiment.oracle;

import java.util.HashMap;
import java.util.Map;

public class ShovelHistoryInfo {
    private final Map<String, String> expectedResult = new HashMap<>();
    private String repositoryName;
    private String repositoryWebURL;
    private String filePath;
    private String functionName;
    private String functionKey;
    private int functionStartLine;
    private String startCommitName;
    private String branchName;

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

    public int getFunctionStartLine() {
        return functionStartLine;
    }

    public void setFunctionStartLine(int functionStartLine) {
        this.functionStartLine = functionStartLine;
    }

    public String getStartCommitName() {
        return startCommitName;
    }

    public void setStartCommitName(String startCommitName) {
        this.startCommitName = startCommitName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public Map<String, String> getExpectedResult() {
        return expectedResult;
    }
}
