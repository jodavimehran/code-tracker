package org.refactoringrefiner;

import org.refactoringminer.api.RefactoringType;
import org.refactoringrefiner.api.MetaInfo;

import java.util.HashMap;
import java.util.Map;

public class MetaInfoImpl implements MetaInfo {
    private final Map<RefactoringType, Integer> typeCount = new HashMap<>();
    private int minTime = Integer.MAX_VALUE;
    private int maxTime = Integer.MIN_VALUE;

    public void addCommitTime(int commitTime) {
        if (commitTime < minTime)
            minTime = commitTime;

        if (commitTime > maxTime)
            maxTime = commitTime;
    }

    public void addType(RefactoringType type) {
        typeCount.merge(type, 1, Integer::sum);
    }

    public Map<RefactoringType, Integer> getTypeCount() {
        return new HashMap<>(typeCount);
    }

    public int getMinTime() {
        return minTime;
    }

    public int getMaxTime() {
        return maxTime;
    }
}
