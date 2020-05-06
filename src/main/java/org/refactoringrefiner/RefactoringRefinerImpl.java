package org.refactoringrefiner;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringrefiner.api.RefactoringRefiner;
import org.refactoringrefiner.api.Result;

import java.util.List;

/**
 *
 */
public class RefactoringRefinerImpl implements RefactoringRefiner {

    public static RefactoringRefiner factory() {
        return new RefactoringRefinerImpl();
    }

    @Override
    public Result analyseAllCommits(Repository repository, String branch) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(repository);
            miner.detectAll(repository, branch, refactoringHandler);
            return new ResultImpl(GraphImpl.of(refactoringHandler.getGraph()), refactoringHandler.getMetaInfo());
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseBetweenTags(Repository repository, String startTag, String endTag) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(repository);
            miner.detectBetweenTags(repository, startTag, endTag, refactoringHandler);
            return new ResultImpl(GraphImpl.of(refactoringHandler.getGraph()), refactoringHandler.getMetaInfo());
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseBetweenCommits(Repository repository, String startCommitId, String endCommitId) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(repository);
            miner.detectBetweenCommits(repository, startCommitId, endCommitId, refactoringHandler);
            return new ResultImpl(GraphImpl.of(refactoringHandler.getGraph()), refactoringHandler.getMetaInfo());
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseCommit(Repository repository, String commitId) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(repository);
            miner.detectAtCommit(repository, commitId, refactoringHandler, 120);
            return new ResultImpl(GraphImpl.of(refactoringHandler.getGraph()), refactoringHandler.getMetaInfo());
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseCommits(Repository repository, List<String> commitList) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(repository);
            for (String commitId : commitList)
                miner.detectAtCommit(repository, commitId, refactoringHandler, 120);
            return new ResultImpl(GraphImpl.of(refactoringHandler.getGraph()), refactoringHandler.getMetaInfo());
        } catch (Exception ex) {
            return null;
        }
    }
}
