package org.codetracker.experiment.oracle.history;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractHistoryInfo {
    private final List<ChangeHistory> expectedChanges = new ArrayList<>();
    private String repositoryName;
    private String repositoryWebURL;
    private String startCommitId;
    private String filePath;
    private String branchName;

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

    public String getStartCommitId() {
        return startCommitId;
    }

    public void setStartCommitId(String startCommitId) {
        this.startCommitId = startCommitId;
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

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    @JsonIgnore
    public abstract String getElementKey();
}
