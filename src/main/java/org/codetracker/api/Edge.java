package org.codetracker.api;

import org.codetracker.change.Change;

import java.util.LinkedHashSet;

public interface Edge {
    LinkedHashSet<Change> getChangeList();

    Change.Type getType();

}
