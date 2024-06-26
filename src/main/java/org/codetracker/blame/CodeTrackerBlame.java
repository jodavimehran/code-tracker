package org.codetracker.blame;

import org.codetracker.api.CodeElement;
import org.codetracker.rest.changeHistory.RESTChange;
import org.codetracker.util.CodeElementLocator;
import org.eclipse.jgit.lib.Repository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.codetracker.blame.Utils.getFileContentByCommit;
import static org.codetracker.rest.RESTHandler.trackCodeHistory;

/* Created by pourya on 2024-06-26*/
public class CodeTrackerBlame implements IBlame{
    private static final String NOT_FOUND_PLACEHOLDER = "-----";

    public List<String[]> blameFile(Repository repository, String commitId, String filePath) throws Exception {
        List<String> lines = getFileContentByCommit(repository, commitId, filePath);
        int maxLine = lines.size();
        List<String[]> result = new ArrayList<>();
        for (int lineNumber = 0; lineNumber < maxLine; lineNumber++) {
            try {
                result.add(lineBlameFormat(repository, commitId, filePath, null, lineNumber, lines));
            }
            catch (Exception e) {
//                System.out.println("Error in line " + lineNumber + ": " + e.getMessage());
            }

        }
        return result;
    }

    private static String[] lineBlameFormat(Repository repository, String commitId, String filePath, String name, int lineNumber, List<String> lines) {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String shortCommitId = NOT_FOUND_PLACEHOLDER;
        String committer = NOT_FOUND_PLACEHOLDER;
        Long commitTime = 0L;
        String beforeFilePath = NOT_FOUND_PLACEHOLDER;
        if (codeElement != null) {
            ArrayList<RESTChange> restChanges = trackCodeHistory(
                    repository,
                    filePath,
                    commitId,
                    name,
                    lineNumber,
                    codeElement
            );

            if (restChanges.isEmpty()) {
                throw new RuntimeException("No history found for the given code element");
            }
            RESTChange h0 = restChanges.get(0);
            shortCommitId = h0.commitId.substring(0, 7);  // take the first 7 characters
            beforeFilePath = h0.elementFileBefore;
            committer = h0.committer;
            commitTime =  h0.commitTime;
        }
        String commitDate =  (commitTime == 0) ? NOT_FOUND_PLACEHOLDER : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date(commitTime * 1000L));
        return new String[]
                {
                        shortCommitId,
                        beforeFilePath,
                        "(" + committer,
                        commitDate,
                        lineNumber + ")",
                        lines.get(lineNumber)
                };
    }

}
