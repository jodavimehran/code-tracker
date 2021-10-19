package org.codetracker.api;

public interface History<C extends CodeElement> {
    Graph<C, Edge> getGraph();

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
