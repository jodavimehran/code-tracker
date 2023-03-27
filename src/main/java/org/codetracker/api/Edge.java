package org.codetracker.api;

import org.codetracker.change.Change;

import java.util.Set;

public interface Edge {
    Set<Change> getChangeList();

    Change.Type getType();

}
