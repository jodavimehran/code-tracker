package org.refactoringrefiner.api;

public interface History<N extends CodeElement, E extends Edge> {
    Graph<N, E> getGraph();
    HistoryReport getHistoryReport();

    interface HistoryReport {
        int getAnalysedCommits();
    }
}
