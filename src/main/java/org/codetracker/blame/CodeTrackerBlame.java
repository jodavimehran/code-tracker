package org.codetracker.blame;

import org.codetracker.api.CodeElement;
import org.codetracker.api.History;
import org.codetracker.util.CodeElementLocator;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.codetracker.blame.Utils.getFileContentByCommit;

/* Created by pourya on 2024-06-26*/
public class CodeTrackerBlame implements IBlame{
    private final static Logger logger = LoggerFactory.getLogger(CodeTrackerBlame.class);
    private static final String NOT_FOUND_PLACEHOLDER = "-----";

    public List<String[]> blameFile(Repository repository, String commitId, String filePath) throws Exception {
        List<String> lines = getFileContentByCommit(repository, commitId, filePath);
        int maxLine = lines.size();
        List<String[]> result = new ArrayList<>();
        for (int lineNumber = 1; lineNumber <= maxLine; lineNumber++) {
            try {
                result.add(lineBlameFormat(repository, commitId, filePath, null, lineNumber, lines));
            } catch (Exception e) {
//                System.out.println(e.getMessage());
            }
        }
        return result;
    }

    public static String[] lineBlameFormat(Repository repository, String commitId, String filePath, String name, int lineNumber, List<String> lines) {
        History.HistoryInfo<? extends CodeElement> latestChange = getLineBlame(repository, commitId, filePath, name, lineNumber);
        String shortCommitId = NOT_FOUND_PLACEHOLDER;
        String committer = NOT_FOUND_PLACEHOLDER;
        String commitDate = NOT_FOUND_PLACEHOLDER;
        String beforeFilePath = NOT_FOUND_PLACEHOLDER;
        long commitTime = 0L;
        if (latestChange != null) {
            shortCommitId = latestChange.getCommitId().substring(0, 9);  // take the first 7 characters
            beforeFilePath = latestChange.getElementBefore().getFilePath();
            committer = latestChange.getCommitterName();
            commitTime =  latestChange.getCommitTime();
            commitDate =  (commitTime == 0) ? NOT_FOUND_PLACEHOLDER : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date(commitTime * 1000L));
        }
        return new String[]
            {
                shortCommitId,
                beforeFilePath,
                "(" + committer,
                commitDate,
                lineNumber + ")",
                lines.get(lineNumber-1)
            };
    }
    public static History.HistoryInfo<? extends CodeElement> getLineBlame(Repository repository, String commitId, String filePath, String name, int lineNumber) {
        CodeElement codeElement = locate(repository, commitId, filePath, name, lineNumber);
        History.HistoryInfo<? extends CodeElement> history = null;
        if (codeElement != null) {
            try {
                history = new LineTrackerImpl().blame(repository, filePath, commitId, name, lineNumber, codeElement);
            } catch (Exception e) {
                logger.error("Error in tracking line blame for " + filePath + " at line " + lineNumber + " in commit " + commitId);
            }
        }
        else {
            logger.error("Code element not found for " + filePath + " at line " + lineNumber + " in commit " + commitId);
        }
        return history;
    }

    private static CodeElement locate(Repository repository, String commitId, String filePath, String name, int lineNumber) {
        CodeElementLocator locator = new CodeElementLocator(
                repository,
                commitId,
                filePath,
                name,
                lineNumber
        );
        CodeElement codeElement = null;
        try {
            codeElement = locator.locate();
        }
        catch (Exception e) {

        }
        return codeElement;
    }

}
