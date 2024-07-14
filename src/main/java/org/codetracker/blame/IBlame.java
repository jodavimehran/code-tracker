package org.codetracker.blame;

import org.codetracker.blame.model.LineBlameResult;
import org.eclipse.jgit.lib.Repository;

import java.util.List;

/* Created by pourya on 2024-06-26*/
public interface IBlame {
    List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath) throws Exception;
    List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath, int fromLine, int toLine) throws Exception;
}
