package org.codetracker.blame.impl;

import org.codetracker.blame.fromDiffer.IDiffToBlame;
import org.codetracker.blame.fromDiffer.NaiveDiffToBlame;
import org.codetracker.blame.model.IBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.eclipse.jgit.lib.Repository;

import java.util.*;

/* Created by pourya on 2024-10-08*/
public class LHDiffBlame implements IBlame {

    private final LHDiffer differ;
    private final IDiffToBlame diffToBlame;

    public LHDiffBlame(IDiffToBlame diffToBlame){
        this.differ = new LHDiffer();
        this.diffToBlame = diffToBlame;
    }
    public LHDiffBlame(){
        this.differ = new LHDiffer();
        this.diffToBlame = new NaiveDiffToBlame(true);
    }
    public LHDiffBlame(LHDiffer differ, IDiffToBlame diffToBlame) {
        this.differ = differ;
        this.diffToBlame = diffToBlame;
    }

    @Override
    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath) throws Exception {
        return diffToBlame.blameFile(differ, repository, commitId, filePath);
    }

    @Override
    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath, int fromLine, int toLine) throws Exception {
        List<LineBlameResult> lineBlameResults = this.blameFile(repository, commitId, filePath);
        List<LineBlameResult> result = new ArrayList<>();
        for (LineBlameResult lineBlameResult : lineBlameResults) {
            if (fromLine <= lineBlameResult.getOriginalLineNumber() && lineBlameResult.getOriginalLineNumber() <= toLine) {
                result.add(lineBlameResult);
            }
        }
        return result;
    }
}