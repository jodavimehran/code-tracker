package org.codetracker.blame.impl;

import org.codetracker.blame.IBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import java.util.ArrayList;
import java.util.List;

public class GitBlame implements IBlame {

    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath) throws Exception {
        List<LineBlameResult> blameList = new ArrayList<>();
        try (Git git = new Git(repository)) {
            ObjectId commit = repository.resolve(commitId);

            BlameCommand blameCommand = git.blame();
            blameCommand.setStartCommit(commit);
            blameCommand.setFilePath(filePath);

            BlameResult blameResult = blameCommand.call();

            if (blameResult != null) {
                int lines = blameResult.getResultContents().size();

                for (int i = 0; i < lines; i++) {
                    blameList.add(LineBlameResult.of(blameResult, i));
                }
            }
        } catch (RevisionSyntaxException | MissingObjectException e) {
            throw new Exception("Failed to resolve the commit ID", e);
        }

        return blameList;
    }
}
