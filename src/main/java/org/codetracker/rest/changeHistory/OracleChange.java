package org.codetracker.rest.changeHistory;

import org.codetracker.api.CodeElement;

// CodeTracker History Element For Oracle Generation
public class OracleChange {

    public String parentCommitId;
    public String commitId;
    public Long commitTime;
    public String changeType;
    public String elementFileBefore;
    public String elementNameBefore;
    public String elementFileAfter;
    public String elementNameAfter;
    public String comment;

    public OracleChange(
            String parentCommitId,
            String commitId,
            Long commitTime,
            String changeType,
            CodeElement before,
            CodeElement after,
            String comment
    ) {
        this.parentCommitId = parentCommitId;
        this.commitId = commitId;
        this.commitTime = commitTime;
        this.changeType = changeType;

        this.elementNameBefore = before.getName();
        this.elementFileBefore = before.getFilePath();

        this.elementNameAfter = after.getName();
        this.elementFileAfter = after.getFilePath();

        this.comment = comment;
    }

    public Long getCommitTime() {
        return commitTime;
    }

    public String getChangeType() {
        return changeType;
    }

    public String getComment() {
        return comment;
    }
}