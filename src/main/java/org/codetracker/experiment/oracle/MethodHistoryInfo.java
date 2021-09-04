package org.codetracker.experiment.oracle;

import java.util.ArrayList;
import java.util.List;

public class MethodHistoryInfo {
    private String repositoryName;
    private String repositoryWebURL;
    private String startCommitId;
    private String filePath;
    private String functionName;
    private String functionKey;
    private int functionStartLine;

    private final List<ChangeHistory> expectedChanges = new ArrayList<>();

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

    public int getFunctionStartLine() {
        return functionStartLine;
    }

    public void setFunctionStartLine(int functionStartLine) {
        this.functionStartLine = functionStartLine;
    }

    public List<ChangeHistory> getExpectedChanges() {
        return expectedChanges;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }
}
