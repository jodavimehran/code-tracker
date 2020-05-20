package org.refactoringrefiner.util;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;

public abstract class BaseRepositoryResolver {
    private final Repository repository;

    public BaseRepositoryResolver(Repository repository) {
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
}
