package org.codetracker.blame.fromDiffer;

import org.codetracker.blame.impl.IDiffer;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.blame.util.Utils;
import org.eclipse.jgit.lib.Repository;

import java.util.*;

import static org.codetracker.blame.util.Utils.*;

/* Created by pourya on 2024-10-09*/
public abstract class AbstractDiffToBlameImpl implements IDiffToBlame {

    final boolean ignore_whitespace;

    protected AbstractDiffToBlameImpl(boolean ignoreWhitespace){

        ignore_whitespace = ignoreWhitespace;
    }
    @Override
    public List<LineBlameResult> blameFile(IDiffer differ, Repository repository, String commitId, String filePath) throws Exception {
        return blameFile(differ, repository, commitId, filePath, null, new ArrayList<>());
    }

    protected List<LineBlameResult> blameFile(IDiffer differ, Repository repository, String commitId, String filePath, List<LineBlameResult> lineBlameResults, List<LineBlameResult> todos) throws Exception {
        System.out.println("Blaming file: " + filePath + " at commit: " + commitId);
        boolean firstInvo = false;
        if (lineBlameResults == null) {
            firstInvo = true;
            lineBlameResults = new ArrayList<>();
        }
        if (!firstInvo && todos.isEmpty()) return lineBlameResults;  // If no to-do items, return immediately

        String parentCommitId = findParentCommitId(repository, commitId);
        List<String> fileContentByCommit = Utils.getFileContentByCommit(repository, commitId, filePath);
        String prevCommitCorrespondingFile = getCorrespondingFileInParentCommit(repository, commitId, parentCommitId, filePath);
        List<String> prevCommitCorrespondingFileContent;
        try{
            prevCommitCorrespondingFileContent = getFileContentByCommit(repository, parentCommitId, prevCommitCorrespondingFile);
        }
        catch (Utils.FileNotFoundInThePrevCommitException e)
        {
            //Step 4: it means all the todos must be coming from the commit itself as an introduction
            processTermination(repository, commitId, filePath, lineBlameResults, todos, parentCommitId);
            return lineBlameResults;
        }
        Map<Integer, Integer> diffMap = differ.getDiffMap(fileContentByCommit, prevCommitCorrespondingFileContent);
        if (firstInvo) {
            // Step 1: Process lines present in diffMap (modified lines)
            processModifiedLines(repository, commitId, filePath, lineBlameResults, todos, diffMap, prevCommitCorrespondingFileContent, fileContentByCommit, parentCommitId);

            // Step 2: Process lines NOT in diffMap (added lines)
            processAddedLines(repository, commitId, filePath, lineBlameResults, fileContentByCommit, diffMap, parentCommitId);
        }

        else if (!todos.isEmpty()){
            // Step 3: Process to-do items and trace their history further back
            processTodos(repository, lineBlameResults, todos, diffMap, prevCommitCorrespondingFileContent, fileContentByCommit, parentCommitId);
        }
        //Recursive pass: trace lines in the to-do list further back
        blameFile(differ, repository, parentCommitId, prevCommitCorrespondingFile, lineBlameResults, todos);

        return lineBlameResults;
    }

    protected abstract void processTermination(Repository repository, String commitId, String filePath, List<LineBlameResult> lineBlameResults, List<LineBlameResult> todos, String parentCommitId);

    protected abstract void processModifiedLines(Repository repository, String commitId, String filePath, List<LineBlameResult> lineBlameResults, List<LineBlameResult> todos, Map<Integer, Integer> diffMap, List<String> prevCommitCorrespondingFileContent, List<String> fileContentByCommit, String parentCommitId);

    protected abstract void processAddedLines(Repository repository, String commitId, String filePath, List<LineBlameResult> lineBlameResults, List<String> fileContentByCommit, Map<Integer, Integer> diffMap, String parentCommitId);

    protected abstract void processTodos(Repository repository, List<LineBlameResult> lineBlameResults, List<LineBlameResult> todos, Map<Integer, Integer> diffMap, List<String> prevCommitCorrespondingFileContent, List<String> fileContentByCommit, String parentCommitId);

    protected abstract String getCorrespondingFileInParentCommit(Repository repository, String commitId, String parentCommitId, String filePath) throws Exception;
}
