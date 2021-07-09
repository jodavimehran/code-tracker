package org.refactoringrefiner.edge;

import org.apache.commons.collections4.SetUtils;
import org.refactoringrefiner.api.Change;
import org.refactoringrefiner.api.Edge;

import java.util.*;
import java.util.stream.Collectors;

public class EdgeImpl implements Edge {
    private final Set<Change> changeList = new HashSet<>();
    private final Map<Change.Type, Set<Change>> changeMap = new HashMap<>();

    public void addChange(AbstractChange change) {
        if (Change.Type.NO_CHANGE.equals(change.type) && !changeList.isEmpty())
            return;
        changeList.add(change);
        changeMap.merge(change.type, new HashSet<>(Arrays.asList(change)), SetUtils::union);
    }

    public Change.Type getType() {
        if (changeList.isEmpty()) {
            return Change.Type.NO_CHANGE;
        }
        if (changeList.size() == 1) {
            return changeList.stream().findFirst().get().getType();
        }
        if (changeMap.keySet().size() == 1) {
            Change.Type type = changeMap.keySet().stream().findFirst().get();
            if (!Change.Type.REFACTORED.equals(type))
                return type;
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
        return changeList.stream().map(Change::getType).map(Objects::toString).collect(Collectors.joining(","));
    }
}
