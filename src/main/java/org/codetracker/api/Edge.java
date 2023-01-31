package org.codetracker.api;

import org.codetracker.change.Change;

import java.util.List;

public interface Edge {
    List<Change> getChangeList();

    Change.Type getType();

}
