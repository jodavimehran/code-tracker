package org.codetracker.blame.benchmark;

import org.codetracker.blame.model.LineBlameResult;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/* Created by pourya on 2024-07-15*/
public class LineNumberToCommitIDRecordManager extends BenchmarkRecordManager<Integer, String> {
    public LineNumberToCommitIDRecordManager() {
        super();
    }
    public void diff(EnumMap<BlamerFactory, List<LineBlameResult>> blameResults) {
        for (Map.Entry<BlamerFactory, List<LineBlameResult>> blamerFactoryListEntry : blameResults.entrySet()) {
            for (LineBlameResult result : blamerFactoryListEntry.getValue()) {
                if (result != null) {
                    int lineNumber = result.getLineNumber();
                    this.register(blamerFactoryListEntry.getKey(), lineNumber, result.getShortCommitId());
                }
            }
        }

    }
}
