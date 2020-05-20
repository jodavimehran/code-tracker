package org.refactoringrefiner.util;

import org.eclipse.jgit.lib.Repository;

import java.util.function.Function;

public class RepositoryCommitTimeResolver extends BaseRepositoryResolver implements Function<String, Integer> {

    public RepositoryCommitTimeResolver(Repository repository) {
        super(repository);
    }

    @Override
    public Integer apply(String commitId) {
        return getRevCommit(commitId).getCommitTime();
    }

}
