package org.refactoringrefiner.util;

public interface IRepository {
    String getParentId(String commitId);

    long getCommitTime(String commitId);
}
