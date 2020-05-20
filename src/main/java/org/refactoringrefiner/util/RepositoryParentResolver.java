package org.refactoringrefiner.util;

import org.eclipse.jgit.lib.Repository;

import java.util.function.Function;

public class RepositoryParentResolver extends BaseRepositoryResolver implements Function<String, String> {

    public RepositoryParentResolver(Repository repository) {
        super(repository);
    }


    @Override
    public String apply(String commitId) {
        return getRevCommit(commitId).getParent(0).getId().getName();
    }
}
