package org.codetracker.blame.model;

import org.codetracker.api.CodeElement;
import org.codetracker.api.History;
import org.eclipse.jgit.blame.BlameResult;

/* Created by pourya on 2024-07-02*/
public class LineBlameResult {
    private final String commitId;
    private final String filePath;
    private final String shortCommitId;
    private final String beforeFilePath;
    private final String committer;
    private final long commitDate;
    private final int resultLineNumber;

    private final int originalLineNumber; //This is the original line number (the one that you pass as the input)

    @Override
    public String toString() {
        return "LineBlameResult{" +
                "commitId='" + commitId + '\'' +
                ", filePath='" + filePath + '\'' +
                ", shortCommitId='" + shortCommitId + '\'' +
                ", beforeFilePath='" + beforeFilePath + '\'' +
                ", committer='" + committer + '\'' +
                ", commitDate='" + commitDate + '\'' +
                ", lineNumber=" + resultLineNumber +
                '}';
    }

    public LineBlameResult(String commitId, String filePath, String beforeFilePath, String committer, long commitDate, int resultLineNumber, int originalLineNumber) {
        this.commitId = commitId;
        this.filePath = filePath;
        this.shortCommitId = commitId.substring(0, 9);
        this.beforeFilePath = beforeFilePath;
        this.committer = committer;
        this.commitDate = commitDate;
        this.resultLineNumber = resultLineNumber;
        this.originalLineNumber = originalLineNumber;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getShortCommitId() {
        return shortCommitId;
    }

    public String getBeforeFilePath() {
        return beforeFilePath;
    }

    public String getCommitter() {
        return committer;
    }

    public long getCommitDate() {
        return commitDate;
    }

    public int getResultLineNumber() {
        return resultLineNumber;
    }

    public int getOriginalLineNumber() {
        return originalLineNumber;
    }

    public static LineBlameResult of(History.HistoryInfo<? extends CodeElement> latestChange, int lineNumber) {
        if (latestChange == null) return null;
        int resultLineNumber = latestChange.getElementAfter().getLocation().getStartLine();
        return new LineBlameResult(latestChange.getCommitId(),
                latestChange.getElementAfter().getFilePath(),
                latestChange.getElementBefore().getFilePath(),
                latestChange.getCommitterName(),
                latestChange.getCommitTime(),
                resultLineNumber,
                lineNumber);
    }
    public static LineBlameResult of(BlameResult blameResult, int i) {
        if (blameResult == null || i < 0 || i >= blameResult.getResultContents().size()) return null;
        String commitId = blameResult.getSourceCommit(i).getId().name();
        String committerName = blameResult.getSourceCommit(i).getAuthorIdent().getName();
        int resultLineNumber = blameResult.getSourceLine(i);
        long commitTime = blameResult.getSourceCommit(i).getCommitTime();
        String filePath = blameResult.getSourcePath(i);
        return new LineBlameResult(commitId, blameResult.getResultPath(), filePath, committerName, commitTime, resultLineNumber, i + 1);
    }
}
