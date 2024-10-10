package org.codetracker.blame.impl;

import org.codetracker.blame.util.Utils;

import java.util.List;
import java.util.Map;

public interface IDiffer{
    Map<Integer, Integer> getDiffMap(List<String> fileContentByCommit, List<String> prevCommitCorrespondingFile);

    default Map<Integer, Integer> getDiffMap(String fileContentByCommit, String prevCommitCorrespondingFile){
        return this.getDiffMap(
            Utils.contentToLines(fileContentByCommit),
            Utils.contentToLines(prevCommitCorrespondingFile)
        );
    }
}
