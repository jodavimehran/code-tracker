package org.refactoringrefiner;

import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.api.Graph;
import org.refactoringrefiner.api.History;

public class HistoryImpl<N extends CodeElement, E extends Edge> implements History<N, E> {
    private final Graph<N, E> graph;
    private final HistoryReportImpl historyReport;

    public HistoryImpl(Graph<N, E> graph, HistoryReportImpl historyReport) {
        this.graph = graph;
        this.historyReport = historyReport;
    }

    @Override
    public Graph<N, E> getGraph() {
        return graph;
    }

    @Override
    public HistoryReport getHistoryReport() {
        return historyReport;
    }

    public static class HistoryReportImpl implements HistoryReport {

        private int analysedCommits = 0;
        private int gitLogCommandCalls = 0;
        private int step2 = 0;
        private int step3 = 0;
        private int step4 = 0;
        private int step5 = 0;

        public int getAnalysedCommits() {
            return analysedCommits;
        }

        public void analysedCommitsPlusPlus() {
            analysedCommits++;
        }

        public void gitLogCommandCallsPlusPlus() {
            gitLogCommandCalls++;
        }

        public int getGitLogCommandCalls() {
            return gitLogCommandCalls;
        }

        public int getStep2() {
            return step2;
        }

        public int getStep3() {
            return step3;
        }

        public int getStep4() {
            return step4;
        }

        public int getStep5() {
            return step5;
        }

        public void step2PlusPlus() {
            step2++;
        }

        public void step3PlusPlus() {
            step3++;
        }

        public void step4PlusPlus() {
            step4++;
        }

        public void step5PlusPlus() {
            step5++;
        }
    }
}
