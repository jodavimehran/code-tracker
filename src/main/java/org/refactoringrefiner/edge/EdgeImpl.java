package org.refactoringrefiner.edge;

import org.refactoringrefiner.api.Change;
import org.refactoringrefiner.api.Edge;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class EdgeImpl implements Edge {
    private final Set<Change> changeList = new HashSet<>();

    public void addChange(AbstractChange change) {
        changeList.add(change);
    }

    public Change.Type getType() {
        if (changeList.isEmpty()) {
            return Change.Type.NO_CHANGE;
        }
        if (changeList.size() == 1) {
            for (Change change : changeList)
                return change.getType();
        }
        return Change.Type.MULTI_CHANGE;
    }

    @Override
    public Set<Change> getChangeList() {
        return changeList;
    }

    @Override
    public String toString() {
        return changeList.stream().map(Objects::toString).collect(Collectors.joining(","));
    }

    @Override
    public String toSummary() {
        return changeList.stream().map(Change::toSummary).collect(Collectors.joining(","));
    }
}
