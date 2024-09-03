package org.codetracker.blame.benchmark;

import org.codetracker.api.CodeElement;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.blame.util.Utils;
import org.codetracker.util.CodeElementLocator;
import org.eclipse.jgit.lib.Repository;

import java.util.*;
import java.util.function.BiPredicate;

/* Created by pourya on 2024-07-14*/
public class BlameDiffer {

    protected final EnumSet<BlamerFactory> blamerFactories;
    protected LineNumberToCommitIDRecordManager benchmarkRecordManager;
    protected BiPredicate<Integer, List<String>> emptyLinesCondition = (lineNumber, content) -> content.get(lineNumber-1).trim().isEmpty();

    public BlameDiffer(EnumSet<BlamerFactory> blamerFactories){
        this.blamerFactories = blamerFactories;
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
        table.entrySet().removeIf(entry -> emptyLinesCondition.test(entry.getKey(), content));
        int legitSize = table.size();
        table =  process(repository, commitId, filePath, table);
        return new BlameDifferResult(table, makeCodeElementMap(table.keySet(), repository, commitId, filePath), legitSize);
    }

    protected Map<Integer, EnumMap<BlamerFactory, String>> process(Repository repository, String commitId, String filePath, Map<Integer, EnumMap<BlamerFactory, String>> table) {
        table.entrySet().removeIf(entry -> entry.getValue().values().stream().distinct().count() == 1);
        return table;
    }

    private EnumMap<BlamerFactory, List<LineBlameResult>> prepareResults(Repository repository, String commitId, String filePath) throws Exception {
        EnumMap<BlamerFactory, List<LineBlameResult>> blameResults = runBlamers(repository, commitId, filePath);
        verify(blameResults);
        return blameResults;
    }

    Map<Integer, CodeElement> makeCodeElementMap(Set<Integer> lineNumbers, Repository repository, String commitId, String filePath){
        Map<Integer, CodeElement> codeElementMap = new LinkedHashMap<>();
        for (Integer lineNumber : lineNumbers) {
            try {
                codeElementMap.put(lineNumber,
                        new CodeElementLocator(repository, commitId, filePath, lineNumber ).locate());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return codeElementMap;
    }
}
