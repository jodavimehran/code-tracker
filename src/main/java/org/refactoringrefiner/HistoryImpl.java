package org.refactoringrefiner;

import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.api.Graph;
import org.refactoringrefiner.api.History;

public class HistoryImpl<N extends CodeElement, E extends Edge> implements History<N, E> {
    private final Graph<N, E> graph;

    public HistoryImpl(Graph<N, E> graph) {
        this.graph = graph;
    }

    @Override
    public Graph<N, E> getGraph() {
        return graph;
    }

}
