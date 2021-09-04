package org.refactoringrefiner.test.oracle;

public class ChangeHistory {
    private String parentCommitId;
    private String commitId;
    private String altCommitId;
    private long commitTime;
    private String changeType;
    private String elementFileBefore;
    private String elementNameBefore;
    private String elementFileAfter;
    private String elementNameAfter;
    private String comment;

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public long getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(long commitTime) {
        this.commitTime = commitTime;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getElementFileBefore() {
        return elementFileBefore;
    }

    public void setElementFileBefore(String elementFileBefore) {
        this.elementFileBefore = elementFileBefore;
    }

    public String getElementNameBefore() {
        return elementNameBefore;
    }

    public void setElementNameBefore(String elementNameBefore) {
        this.elementNameBefore = elementNameBefore;
    }

    public String getElementFileAfter() {
        return elementFileAfter;
    }

    public void setElementFileAfter(String elementFileAfter) {
        this.elementFileAfter = elementFileAfter;
    }

    public String getElementNameAfter() {
        return elementNameAfter;
    }

    public void setElementNameAfter(String elementNameAfter) {
        this.elementNameAfter = elementNameAfter;
    }

    public String getParentCommitId() {
        return parentCommitId;
    }

    public void setParentCommitId(String parentCommitId) {
        this.parentCommitId = parentCommitId;
    }

    public String getAltCommitId() {
        return altCommitId;
    }

    public void setAltCommitId(String altCommitId) {
        this.altCommitId = altCommitId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
