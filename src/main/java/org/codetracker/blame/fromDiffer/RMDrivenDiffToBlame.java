package org.codetracker.blame.fromDiffer;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

/* Created by pourya on 2024-10-09*/
public class RMDrivenDiffToBlame extends BaseDiffToBlameImpl {
    public RMDrivenDiffToBlame(boolean ignoreWhitespace) {
        super(ignoreWhitespace);
    }

    @Override
    protected String getCorrespondingFileInParentCommit(Repository repository, String commitId, String parentCommitId, String filePath) throws Exception {
        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommit(repository, commitId);
        if (projectASTDiff == null) return filePath;
        for (ASTDiff astDiff : projectASTDiff.getDiffSet()) {
            if (astDiff.getDstPath().equals(filePath)) {
                return astDiff.getSrcPath();
            }
        }

        return filePath;
    }

}
