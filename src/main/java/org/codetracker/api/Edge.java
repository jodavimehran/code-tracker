package org.refactoringrefiner.api;

import java.util.Set;

public interface Edge {
    Set<Change> getChangeList();

    Change.Type getType();

    String toSummary();
}
