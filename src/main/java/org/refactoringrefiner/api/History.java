package org.refactoringrefiner.api;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface History<N extends CodeElement, E extends Edge> {
    Graph<N, E> getGraph();
}
