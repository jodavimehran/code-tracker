package org.refactoringrefiner.util;

import org.refactoringrefiner.VersionImpl;

public interface IRepository {
    String getParentId(String commitId);

    long getCommitTime(String commitId);

    default VersionImpl getVersion(String commitId) {
        return new VersionImpl(commitId, getCommitTime(commitId));
    }
}
