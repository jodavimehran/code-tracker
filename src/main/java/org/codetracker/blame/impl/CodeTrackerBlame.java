package org.codetracker.blame.impl;

import org.codetracker.api.CodeElement;
import org.codetracker.api.History;
import org.codetracker.blame.IBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.blame.adaptor.LineTrackerFromCodeTracker;
import org.codetracker.util.CodeElementLocator;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.codetracker.blame.util.Utils.getFileContentByCommit;

/* Created by pourya on 2024-06-26*/
public class CodeTrackerBlame implements IBlame {
    private final static Logger logger = LoggerFactory.getLogger(CodeTrackerBlame.class);

    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath) throws Exception {
        List<String> lines = getFileContentByCommit(repository, commitId, filePath);
        int maxLine = lines.size();
        List<LineBlameResult> result = new ArrayList<>();
        for (int lineNumber = 1; lineNumber <= maxLine; lineNumber++) {
            try {
                result.add(getBlameInfo(repository, commitId, filePath, lineNumber));
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return result;
    }


    private LineBlameResult getBlameInfo(Repository repository, String commitId, String filePath, int lineNumber) {
        History.HistoryInfo<? extends CodeElement> latestChange = getLineBlame(repository, commitId, filePath, lineNumber);
        return LineBlameResult.of(latestChange);
    }


    public History.HistoryInfo<? extends CodeElement> getLineBlame(Repository repository, String commitId, String filePath, int lineNumber) {
        CodeElement codeElement = locate(repository, commitId, filePath, lineNumber);
        History.HistoryInfo<? extends CodeElement> history = null;
        if (codeElement != null) {
            try {
                history = new LineTrackerFromCodeTracker().blame(repository, filePath, commitId, lineNumber, codeElement);
            } catch (Exception e) {
                logger.error("Error in tracking line blame for " + filePath + " at line " + lineNumber + " in commit " + commitId);
                logger.error(e.getMessage());
            }
        }
        else {
            logger.error("Code element not found for " + filePath + " at line " + lineNumber + " in commit " + commitId);
        }
        return history;
    }

    private CodeElement locate(Repository repository, String commitId, String filePath, int lineNumber) {
        CodeElementLocator locator = new CodeElementLocator(
                repository,
                commitId,
                filePath,
                lineNumber
        );
        CodeElement codeElement = null;
        try {
            codeElement = locator.locate();
        }
        catch (Exception e) {
            logger.error("Error in locating code element for " + filePath + " at line " + lineNumber + " in commit " + commitId);
            logger.error(e.getMessage());
        }
        return codeElement;
    }

}
