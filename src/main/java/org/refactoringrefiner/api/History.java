package org.refactoringrefiner.api;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface History {
    List<Pair<Version, Edge>> getEventList();

    int getNumberOfCommit();
}
