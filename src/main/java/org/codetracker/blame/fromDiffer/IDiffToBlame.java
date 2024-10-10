package org.codetracker.blame.fromDiffer;

import org.codetracker.blame.impl.IDiffer;
import org.codetracker.blame.model.IBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.eclipse.jgit.lib.Repository;

import java.util.List;

/* Created by pourya on 2024-10-09*/
public interface IDiffToBlame {
    List<LineBlameResult> blameFile(IDiffer differ, Repository repository, String commitId, String filePath) throws Exception;

    static boolean identicalStrings(boolean ignore_whitespace, String prevContent, String currContent) {
        if (ignore_whitespace)
            //FIXME: have to verify the logic on the trimming instead of "\s" replaceAll => return prevContent.replaceAll("\\s", "").equals(currContent.replaceAll("\\s", ""));
            return prevContent.trim().equals(currContent.trim());
        else
            return prevContent.equals(currContent);
    }
}
