package org.codetracker.change;

import org.apache.commons.collections4.SetUtils;
import org.codetracker.api.Edge;

import java.util.*;
import java.util.stream.Collectors;

public class EdgeImpl implements Edge {
    private final Set<Change> changeList = new HashSet<>();
    private final Map<Change.Type, Set<Change>> changeMap = new HashMap<>();

    public void addChange(AbstractChange change) {
        if (Change.Type.NO_CHANGE.equals(change.getType()) && !changeList.isEmpty())
            return;
        if (Change.Type.BODY_CHANGE.equals(change.getType()) && changeMap.containsKey(Change.Type.BODY_CHANGE))
            return;
        if (Change.Type.DOCUMENTATION_CHANGE.equals(change.getType()) && changeMap.containsKey(Change.Type.DOCUMENTATION_CHANGE))
            return;
        if (Change.Type.CONTAINER_CHANGE.equals(change.getType()) && changeMap.containsKey(Change.Type.CONTAINER_CHANGE))
            return;
        changeList.add(change);
        changeMap.merge(change.getType(), new HashSet<>(Arrays.asList(change)), SetUtils::union);
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
            return type;
        }
        return Change.Type.MULTI_CHANGE;
    }

    @Override
    public Set<Change> getChangeList() {
        List<Change> changeListArray = new ArrayList<Change>(changeList);
        changeListArray.sort(Comparator.comparing(Change::getType));
        Set<Change> changeHashSet = new LinkedHashSet<Change>(changeListArray);
        return changeHashSet;
    }

    @Override
    public String toString() {
        return changeList.stream().map(Objects::toString).collect(Collectors.joining(","));
    }

    public String toSummary() {
        return changeList.stream().map(Change::getType).map(Objects::toString).collect(Collectors.joining(","));
    }
}
