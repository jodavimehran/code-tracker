package org.codetracker.blame.fromDiffer;

import org.codetracker.blame.model.LineBlameResult;
import org.eclipse.jgit.lib.Repository;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.codetracker.blame.util.Utils.findCommitDate;
import static org.codetracker.blame.util.Utils.findCommitter;

/* Created by pourya on 2024-10-09*/
public abstract class BaseDiffToBlameImpl extends AbstractDiffToBlameImpl {

    protected BaseDiffToBlameImpl(boolean ignoreWhitespace) {
        super(ignoreWhitespace);
    }

    protected void processTermination(Repository repository, String commitId, String filePath, List<LineBlameResult> lineBlameResults, List<LineBlameResult> todos, String parentCommitId) {
        Iterator<LineBlameResult> iterator = todos.iterator(); // Use an iterator to safely remove items from the list
        while (iterator.hasNext()) {
            LineBlameResult todo = iterator.next();
            lineBlameResults.add(new LineBlameResult(
                    commitId, filePath, filePath,
                    findCommitter(repository, commitId), parentCommitId, findCommitDate(repository, commitId),
                    todo.getResultLineNumber(), todo.getResultLineNumber()
            ));
            iterator.remove();
        }
    }

    protected void processTodos(Repository repository, List<LineBlameResult> lineBlameResults, List<LineBlameResult> todos, Map<Integer, Integer> diffMap, List<String> prevCommitCorrespondingFileContent, List<String> fileContentByCommit, String parentCommitId) {
        Iterator<LineBlameResult> iterator = todos.iterator(); // Use an iterator to safely remove items from the list
        while (iterator.hasNext()) {
            LineBlameResult lineBlameResult = iterator.next();
            Integer prevLine = diffMap.get(lineBlameResult.getResultLineNumber());

            if (prevLine != null) {
                String prevContent = prevCommitCorrespondingFileContent.get(prevLine);
                String currContent = fileContentByCommit.get(lineBlameResult.getResultLineNumber());

                // If the content matches, continue tracing back
                if (IDiffToBlame.identicalStrings(ignore_whitespace, prevContent, currContent)) {
                    lineBlameResult.setCommitId(parentCommitId);
                    lineBlameResult.setCommitDate(findCommitDate(repository, parentCommitId));
                    lineBlameResult.setCommitter(findCommitter(repository, parentCommitId));
                } else {
                    // Blame this line on the current commit if content has changed
                    System.out.println("Found new ones");
                    lineBlameResults.add(lineBlameResult);
                    // Remove the processed line from to-do
                    iterator.remove();
                }
            }
        }
    }

    protected void processAddedLines(Repository repository, String commitId, String filePath, List<LineBlameResult> lineBlameResults, List<String> fileContentByCommit, Map<Integer, Integer> diffMap, String parentCommitId) {
        for (int i = 0; i < fileContentByCommit.size(); i++) {
            if (!diffMap.containsKey(i)) {  // If line is not part of the diffMap
                String currContent = fileContentByCommit.get(i);
                String committer = findCommitter(repository, commitId);
                long commitDate = findCommitDate(repository, commitId);
                LineBlameResult lineBlameResult = new LineBlameResult(
                        commitId, filePath, filePath,
                        committer, parentCommitId, commitDate,
                        i, i
                );
                // Directly blame the parent commit for unchanged lines
                lineBlameResults.add(lineBlameResult);
            }
        }
    }

    protected void processModifiedLines(Repository repository, String commitId, String filePath, List<LineBlameResult> lineBlameResults, List<LineBlameResult> todos, Map<Integer, Integer> diffMap, List<String> prevCommitCorrespondingFileContent, List<String> fileContentByCommit, String parentCommitId) {
        for (Map.Entry<Integer, Integer> lineNumber_lineNumber : diffMap.entrySet()) {
            Integer prevLine = lineNumber_lineNumber.getValue();
            Integer currLine = lineNumber_lineNumber.getKey();
            String prevContent = prevCommitCorrespondingFileContent.get(prevLine);
            String currContent = fileContentByCommit.get(currLine);
            String committer = findCommitter(repository, commitId);
            long commitDate = findCommitDate(repository, commitId);
            LineBlameResult lineBlameResult = new LineBlameResult(
                    commitId, filePath, filePath,
                    committer, parentCommitId, commitDate,
                    prevLine, currLine
            );
            // If lines are identical (ignoring whitespace), add to to-do for further tracing
            if (IDiffToBlame.identicalStrings(ignore_whitespace, prevContent, currContent)) {
                todos.add(lineBlameResult);  // Add for further recursion
            } else {
                lineBlameResults.add(lineBlameResult);  // Blame on current commit
            }
        }
    }

}
