package org.codetracker.blame.impl;

import com.srlab.matching.LHDiff;

import java.util.HashMap;
import java.util.List;

/* Created by pourya on 2024-10-09*/
public class LHDiffer implements IDiffer {

    private final LHDiff lhDiff;
    public LHDiffer() {
        this.lhDiff = new LHDiff();
    }
    public LHDiffer(LHDiff lhDiff) {
        this.lhDiff = lhDiff;
    }
    @Override
    public HashMap<Integer, Integer> getDiffMap(List<String> fileContentByCommit, List<String> prevCommitCorrespondingFile) {

        lhDiff.init(fileContentByCommit, prevCommitCorrespondingFile);
        lhDiff.match();
        return lhDiff.getDiffMap();
    }
}
