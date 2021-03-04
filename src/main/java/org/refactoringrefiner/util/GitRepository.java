package org.refactoringrefiner.util;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;

public class GitRepository implements IRepository {
    private final Repository repository;

    public GitRepository(Repository repository) {
        this.repository = repository;
    }

    protected RevCommit getRevCommit(String commitId) {
        RevCommit revCommit = null;
        try {
            revCommit = repository.parseCommit(ObjectId.fromString(commitId));
        } catch (IncorrectObjectTypeException e) {
            e.printStackTrace();
        } catch (MissingObjectException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return revCommit;
    }

    @Override
    public String getParentId(String commitId) {
        return getRevCommit(commitId).getParent(0).getId().getName();
    }

    @Override
    public long getCommitTime(String commitId) {
        return getRevCommit(commitId).getCommitTime();
    }
}
