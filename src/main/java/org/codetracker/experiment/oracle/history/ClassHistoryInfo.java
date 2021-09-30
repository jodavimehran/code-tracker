package org.codetracker.experiment.oracle.history;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"repositoryName", "repositoryWebURL", "startCommitId", "filePath", "branchName", "className", "classKey", "classDeclarationLine", "expectedChanges"})
public class ClassHistoryInfo extends AbstractHistoryInfo {
    private String className;
    private String classKey;
    private int classDeclarationLine;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassKey() {
        return classKey;
    }

    public void setClassKey(String classKey) {
        this.classKey = classKey;
    }

    public int getClassDeclarationLine() {
        return classDeclarationLine;
    }

    public void setClassDeclarationLine(int classDeclarationLine) {
        this.classDeclarationLine = classDeclarationLine;
    }

    @Override
    public String getElementKey() {
        return classKey;
    }
}
