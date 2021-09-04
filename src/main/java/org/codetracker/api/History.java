package org.refactoringrefiner.api;

public interface History<N extends CodeElement, E extends Edge> {
    Graph<N, E> getGraph();

    HistoryReport getHistoryReport();

    interface HistoryReport {
        int getAnalysedCommits();

        int getGitLogCommandCalls();

        int getStep2();

        int getStep3();

        int getStep4();

        int getStep5();
    }
}
