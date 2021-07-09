package org.refactoringrefiner.test;

public class FileChangeInfo {
    private final String fileName;
    private final String status;
    private final int linesChanged;
    private final int linesAdded;
    private final int linesDeleted;

    public FileChangeInfo(String fileName, String status, int linesChanged, int linesAdded, int linesDeleted) {
        this.fileName = fileName;
        this.status = status;
        this.linesChanged = linesChanged;
        this.linesAdded = linesAdded;
        this.linesDeleted = linesDeleted;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStatus() {
        return status;
    }

    public int getLinesChanged() {
        return linesChanged;
    }

    public int getLinesAdded() {
        return linesAdded;
    }

    public int getLinesDeleted() {
        return linesDeleted;
    }
}
