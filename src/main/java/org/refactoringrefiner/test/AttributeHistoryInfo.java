package org.refactoringrefiner.test;

import java.util.LinkedHashMap;

public class AttributeHistoryInfo {
    private String repositoryName;
    private String repositoryWebURL;
    private String filePath;
    private String attributeName;
    private String attributeKey;
    private String startCommitName;
    private String branchName;
    private LinkedHashMap<String, String> expectedResult;
    private int attributeLine;

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

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
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

    public LinkedHashMap<String, String> getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(LinkedHashMap<String, String> expectedResult) {
        this.expectedResult = expectedResult;
    }

    public int getAttributeLine() {
        return attributeLine;
    }

    public void setAttributeLine(int attributeLine) {
        this.attributeLine = attributeLine;
    }
}
