package org.codetracker.blame.impl;

import org.codetracker.FileTrackerImpl;
import org.codetracker.api.History;
import org.codetracker.blame.model.IBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.element.BaseCodeElement;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/* Created by pourya on 2024-08-22*/
public class FileTrackerBlame implements IBlame {
    private final static Logger logger = LoggerFactory.getLogger(FileTrackerBlame.class);
    @Override
    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath) throws Exception {
        System.out.println("Blaming file " + filePath + " in commit " + commitId);
        return this.blameFile(repository, commitId, filePath, 1, Integer.MAX_VALUE);
    }

    @Override
    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath, int fromLine, int toLine) throws Exception {
        FileTrackerImpl fileTracker = new FileTrackerImpl(repository, commitId, filePath);
        fileTracker.blame();
        List<LineBlameResult> result = new ArrayList<>();
        if (fromLine < 0) {
            fromLine = 1;
        }
        if (toLine > fileTracker.getLines().size()) {
            toLine = fileTracker.getLines().size();
        }
        for (int lineNumber = fromLine; lineNumber <= toLine; lineNumber++) {
            History.HistoryInfo<? extends BaseCodeElement> value = fileTracker.getBlameInfo().get(lineNumber);
            try {
                result.add(LineBlameResult.of(value, lineNumber));
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return result;
    }
}
