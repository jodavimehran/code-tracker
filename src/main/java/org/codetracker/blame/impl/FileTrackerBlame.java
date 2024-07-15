package org.codetracker.blame.impl;

import org.codetracker.FileTrackerImpl;
import org.codetracker.api.History;
import org.codetracker.blame.IBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.element.BaseCodeElement;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* Created by pourya on 2024-08-22*/
public class FileTrackerBlame implements IBlame {
    private final static Logger logger = LoggerFactory.getLogger(FileTrackerBlame.class);
    @Override
    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath) throws Exception {
        FileTrackerImpl fileTracker = new FileTrackerImpl(repository, commitId, filePath);
        fileTracker.blame();
        List<LineBlameResult> result = new ArrayList<>();
        for (Map.Entry<Integer, History.HistoryInfo<? extends BaseCodeElement>> entry : fileTracker.getBlameInfo().entrySet()) {
            Integer lineNumber = entry.getKey();
            History.HistoryInfo<? extends BaseCodeElement> value = entry.getValue();
            try {
                if (fileTracker.getLines().get(lineNumber- 1).isBlank())
                    result.add(null);
                else
                    result.add(LineBlameResult.of(value, lineNumber));
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return result;
    }

    @Override
    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath, int fromLine, int toLine) throws Exception {
        FileTrackerImpl fileTracker = new FileTrackerImpl(repository, commitId, filePath);
        fileTracker.blame();
        List<LineBlameResult> result = new ArrayList<>();
        for (Map.Entry<Integer, History.HistoryInfo<? extends BaseCodeElement>> entry : fileTracker.getBlameInfo().entrySet()) {
            Integer lineNumber = entry.getKey();
            History.HistoryInfo<? extends BaseCodeElement> value = entry.getValue();
            if (lineNumber < fromLine || lineNumber > toLine) {
                continue;
            }
            try {
                if (fileTracker.getLines().get(lineNumber- 1).isBlank())
                    result.add(null);
                else
                    result.add(LineBlameResult.of(value, lineNumber));
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return result;

    }
}
