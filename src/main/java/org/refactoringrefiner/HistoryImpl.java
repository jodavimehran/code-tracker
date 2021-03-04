package org.refactoringrefiner;

import com.google.common.graph.ImmutableValueGraph;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.api.History;
import org.refactoringrefiner.api.Version;
import org.refactoringrefiner.edge.EdgeImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HistoryImpl implements History {
    private final List<Pair<Version, Edge>> eventList = new ArrayList<>();
    private final int numberOfCommit;

    public HistoryImpl(int numberOfCommit) {
        this.numberOfCommit = numberOfCommit;
    }

    public List<Pair<Version, Edge>> getEventList() {
        return eventList;
    }

    public int getNumberOfCommit() {
        return numberOfCommit;
    }

    public void findHistory(CodeElement codeElement, ImmutableValueGraph<CodeElement, Edge> graph) {
        Set<CodeElement> successors = graph.successors(codeElement);
        if (successors.isEmpty())
            return;

        for (CodeElement rightElement : successors) {
            EdgeImpl edge = (EdgeImpl) graph.edgeValue(codeElement, rightElement).get();

            if (!edge.getType().isNoChange()) {
                eventList.add(Pair.of(rightElement.getVersion(), edge));
            }
            findHistory(rightElement, graph);

        }
    }
}
