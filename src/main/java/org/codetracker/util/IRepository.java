package org.codetracker.util;

import org.codetracker.VersionImpl;

public interface IRepository {
    String getParentId(String commitId);

    long getCommitTime(String commitId);

    default VersionImpl getVersion(String commitId) {
        return new VersionImpl(commitId, getCommitTime(commitId));
    }
}
