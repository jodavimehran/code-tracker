package org.codetracker.blame.fromDiffer;

import org.eclipse.jgit.lib.Repository;

/* Created by pourya on 2024-10-09*/
public class NaiveDiffToBlame extends BaseDiffToBlameImpl {
    public NaiveDiffToBlame(boolean ignoreWhitespace) {
        super(ignoreWhitespace);
    }
    @Override
    protected String getCorrespondingFileInParentCommit(Repository repository, String commitId, String parentCommitId, String filePath) {
        return filePath;
    }
}
