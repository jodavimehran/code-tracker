package org.codetracker.blame.benchmark.impl;

import org.codetracker.api.CodeElement;
import org.codetracker.blame.benchmark.BlamerFactory;
import org.codetracker.blame.benchmark.impl.record.LineNumberToCommitIDRecordManager;
import org.codetracker.blame.benchmark.model.BlameDifferResult;
import org.codetracker.blame.model.CodeElementWithRepr;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.blame.util.Utils;
import org.codetracker.util.CodeElementLocator;
import org.eclipse.jgit.lib.Repository;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/* Created by pourya on 2024-07-14*/
public class BlameDiffer {

    protected final EnumSet<BlamerFactory> blamerFactories;
    protected LineNumberToCommitIDRecordManager benchmarkRecordManager;
    protected BiPredicate<Integer, List<String>> emptyLinesCondition = (lineNumber, content) -> content.get(lineNumber-1).trim().isEmpty();
    protected final Predicate<String> ignoreCondition;

    public BlameDiffer(EnumSet<BlamerFactory> blamerFactories, Predicate<String> ignoreCondition){
        this.blamerFactories = blamerFactories;
        this.ignoreCondition = ignoreCondition;
    }

    private EnumMap<BlamerFactory, List<LineBlameResult>> runBlamers(Repository repository, String commitId, String filePath) throws Exception {
        EnumMap<BlamerFactory, List<LineBlameResult>> results = new EnumMap<>(BlamerFactory.class);
        for (BlamerFactory blamerFactory : blamerFactories) {
            System.out.println("Running " + blamerFactory);
            List<LineBlameResult> lineBlameResults = blamerFactory.getBlamer().blameFile
                    (repository, commitId, filePath);
            results.put(blamerFactory, lineBlameResults);
            System.out.println("Finished " + blamerFactory);
        }
        return results;
    }

    protected boolean verify(EnumMap<BlamerFactory, List<LineBlameResult>> results) {
        if (results.size() != 2)
            throw new RuntimeException("BlameDiffer only works with two blamers");
        return true;
    }

    public final BlameDifferResult diff(Repository repository, String commitId, String filePath) throws Exception {
        EnumMap<BlamerFactory, List<LineBlameResult>> blameResults = prepareResults(repository, commitId, filePath);
        List<String> content = Utils.getFileContentByCommit(repository, commitId, filePath);
        benchmarkRecordManager = new LineNumberToCommitIDRecordManager();
        benchmarkRecordManager.diff(blameResults);
        Map<Integer, EnumMap<BlamerFactory, String>> table = benchmarkRecordManager.getRegistry();
        table.entrySet().removeIf(entry  -> ignoreCondition.test(content.get(entry.getKey() - 1)));
        int legitSize = table.size();
        table =  process(repository, commitId, filePath, table);
        return new BlameDifferResult(table, makeCodeElementMap(table.keySet(), repository, commitId, filePath), legitSize);
    }

    protected Map<Integer, EnumMap<BlamerFactory, String>> process(Repository repository, String commitId, String filePath, Map<Integer, EnumMap<BlamerFactory, String>> table) {
        table.entrySet().removeIf(entry -> entry.getValue().values().stream().distinct().count() == 1);
        //TODO: For this one we dont consider the merge commits;
        return table;
    }

    private EnumMap<BlamerFactory, List<LineBlameResult>> prepareResults(Repository repository, String commitId, String filePath) throws Exception {
        EnumMap<BlamerFactory, List<LineBlameResult>> blameResults = runBlamers(repository, commitId, filePath);
        verify(blameResults);
        return blameResults;
    }

    Map<Integer, CodeElementWithRepr> makeCodeElementMap(Set<Integer> lineNumbers, Repository repository, String commitId, String filePath){
        Map<Integer, CodeElementWithRepr> codeElementMap = new LinkedHashMap<>();
        for (Integer lineNumber : lineNumbers) {
            try {
                CodeElement locate = new CodeElementLocator(repository, commitId, filePath, lineNumber).locate();
                codeElementMap.put(lineNumber, new CodeElementWithRepr(locate, Utils.getFileContentByCommit(repository, commitId, filePath).get(lineNumber-1)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return codeElementMap;
    }
}


