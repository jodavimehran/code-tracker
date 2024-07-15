package org.codetracker.blame.benchmark;

import org.codetracker.api.CodeElement;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.blame.util.Utils;
import org.codetracker.util.CodeElementLocator;
import org.eclipse.jgit.lib.Repository;

import java.util.*;
import java.util.function.Predicate;

/* Created by pourya on 2024-07-14*/
public class BlameDiffer {

    private final EnumSet<BlamerFactory> blamerFactories;
    private EnumMap<BlamerFactory, List<LineBlameResult>> blameResults;
    private List<String> content;
    private Predicate<Integer> emptyLinesCondition;
    private Repository repository;
    private String commitId;
    private String filePath;
    private final Map<Integer, CodeElement> codeElementMap = new LinkedHashMap<>();
    private int legitSize;

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

    private static void verify(EnumMap<BlamerFactory, List<LineBlameResult>> blameResults) {
        if (blameResults.size() != 2)
            throw new RuntimeException("BlameDiffer only works with two blamers");
    }

    public Map<Integer, EnumMap<BlamerFactory, String>> diff(Repository repository, String commitId, String filePath) throws Exception {
        this.repository = repository;
        this.commitId = commitId;
        this.filePath = filePath;
        this.blameResults = runBlamers(repository, commitId, filePath);
        this.content = Utils.getFileContentByCommit(repository, commitId, filePath);
        this.emptyLinesCondition = lineNumber -> content.get(lineNumber-1).trim().isEmpty();
        verify(blameResults);
        LineNumberToCommitIDRecordManager benchmarkRecordManager = new LineNumberToCommitIDRecordManager();
        benchmarkRecordManager.diff(blameResults);
        Map<Integer, EnumMap<BlamerFactory, String>> table = benchmarkRecordManager.getRegistry();
        table.entrySet().removeIf(entry -> emptyLinesCondition.test(entry.getKey()));
        legitSize = table.size();
        table.entrySet().removeIf(entry -> entry.getValue().values().stream().distinct().count() == 1);
        makeCodeElementMap(table.keySet());
        return table;
    }

    void makeCodeElementMap(Set<Integer> lineNumbers){
        for (Integer lineNumber : lineNumbers) {
            try {
                codeElementMap.put(lineNumber,
                        new CodeElementLocator(repository, commitId, filePath, lineNumber ).locate());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Map<Integer, CodeElement> getCodeElementMap() {
        return codeElementMap;
    }
    public int getLegitSize() {return legitSize;}
}
