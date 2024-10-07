package org.codetracker.blame.benchmark.impl.record;

import org.codetracker.blame.benchmark.EBlamer;
import org.codetracker.blame.model.IBlameTool;
import org.codetracker.blame.model.LineBlameResult;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/* Created by pourya on 2024-07-15*/
public class LineNumberToCommitIDRecordManager extends BenchmarkRecordManager<Integer, String> {
    public LineNumberToCommitIDRecordManager() {
        super();
    }
    public void diff(Map<IBlameTool, List<LineBlameResult>> blameResults) {
        for (Map.Entry<IBlameTool, List<LineBlameResult>> blamerFactoryListEntry : blameResults.entrySet()) {
            for (LineBlameResult result : blamerFactoryListEntry.getValue()) {
                if (result != null) {
                    int lineNumber = result.getOriginalLineNumber();
                    this.register(blamerFactoryListEntry.getKey(), lineNumber, result.getShortCommitId());
                }
            }
        }

    }
}
