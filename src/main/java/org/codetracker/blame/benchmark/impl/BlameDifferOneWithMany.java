package org.codetracker.blame.benchmark.impl;

import org.codetracker.blame.benchmark.EBlamer;
import org.codetracker.blame.model.IBlameTool;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.blame.util.GithubUtilsWithCache;
import org.eclipse.jgit.lib.Repository;

import java.util.*;
import java.util.function.Predicate;

import static org.codetracker.blame.util.GithubUtils.areSameConsideringMerge;

/* Created by pourya on 2024-09-03*/
public class BlameDifferOneWithMany extends BlameDiffer {

    protected final IBlameTool subject;
    public BlameDifferOneWithMany(Set<IBlameTool> blamerFactories, IBlameTool subject, Predicate<String> codeElementFilter) {
        super(blamerFactories, codeElementFilter);
        this.subject = subject;
    }

    @Override
    protected Map<Integer, Map<IBlameTool, String>> process(Repository repository, String commitId, String filePath, Map<Integer, Map<IBlameTool, String>> table) {
        GithubUtilsWithCache githubUtilsWithCache = new GithubUtilsWithCache(repository);
        table.entrySet().removeIf(entry -> {
            Map<IBlameTool, String> factories = entry.getValue();
            String subject_value = factories.get(subject);
            Predicate<String> stringPredicate = value -> value.equals(subject_value)
//                    || githubUtilsWithCache.areSameConsideringMerge(value, subject_value)
                    ;

            return factories.values().stream().filter(stringPredicate).count() > 1;
        });
        return table;
    }

    @Override
    protected boolean verify(Map<IBlameTool, List<LineBlameResult>> results) {
        return true; //TODO:
    }
}
