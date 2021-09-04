package org.refactoringrefiner;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.api.Graph;
import org.refactoringrefiner.change.AbstractChange;
import org.refactoringrefiner.change.ChangeFactory;
import org.refactoringrefiner.edge.EdgeImpl;
import org.refactoringrefiner.element.BaseCodeElement;

import java.util.*;

public class ChangeHistory<T extends BaseCodeElement> {
    private final MutableValueGraph<T, Edge> changeHistoryGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();

    private void addCodeElementToMap(String name, T codeElement, HashMap<String, Set<T>> elementsMap) {
        Set<T> codeElements;
        if (elementsMap.containsKey(name)) {
            codeElements = elementsMap.get(name);
        } else {
            codeElements = new HashSet<>();
            elementsMap.put(name, codeElements);
        }
        codeElements.add(codeElement);
    }

    public void addChange(T leftSide, T rightSide, ChangeFactory changeFactory) {
        if (leftSide == null || rightSide == null)
            return;
        if (leftSide.equals(rightSide))
            return;
        Optional<Edge> edgeValue = changeHistoryGraph.edgeValue(leftSide, rightSide);
        if (edgeValue.isPresent()) {
            EdgeImpl edge = (EdgeImpl) edgeValue.get();
            edge.addChange(Objects.requireNonNull(changeFactory.build()));
        } else {
            changeHistoryGraph.putEdgeValue(leftSide, rightSide, changeFactory.asEdge());
        }
    }

    public int getNumberOfEdge() {
        return changeHistoryGraph.edges().size();
    }

    public T addNode(T codeElement) {
        changeHistoryGraph.addNode(codeElement);
        return codeElement;
    }

    public void connectRelatedNodes() {
        HashMap<String, Set<T>> leafElementsByIdentifier = new HashMap<>();
        HashMap<String, Set<T>> rootElementByIdentifier = new HashMap<>();

        HashMap<String, Set<T>> leafElementsByName = new HashMap<>();
        HashMap<String, Set<T>> rootElementByName = new HashMap<>();
        for (T node : changeHistoryGraph.nodes()) {
            if (changeHistoryGraph.predecessors(node).isEmpty() && !node.isAdded()) {
                addCodeElementToMap(node.getIdentifierIgnoringVersion(), node, rootElementByIdentifier);
                addCodeElementToMap(node.getName(), node, rootElementByName);
            }
            if (changeHistoryGraph.successors(node).isEmpty() && !node.isRemoved()) {
                addCodeElementToMap(node.getIdentifierIgnoringVersion(), node, leafElementsByIdentifier);
                addCodeElementToMap(node.getName(), node, leafElementsByName);
            }
        }
        matchElements(leafElementsByIdentifier, rootElementByIdentifier);
//        matchElements(leafElementsByName, rootElementByName);
    }

    private void matchElements(HashMap<String, Set<T>> leafElements, HashMap<String, Set<T>> rootElement) {
        for (Map.Entry<String, Set<T>> leafEntry : leafElements.entrySet()) {
            if (!rootElement.containsKey(leafEntry.getKey())) {
                continue;
            }
            List<T> leafCodeElementsList = new ArrayList<>(leafEntry.getValue());
            leafCodeElementsList.sort((o1, o2) -> Long.compare(o2.getVersion().getTime(), o1.getVersion().getTime()));
            Set<T> rootCodeElements = rootElement.get(leafEntry.getKey());
            for (T leafCodeElement : leafCodeElementsList) {
                if (!changeHistoryGraph.successors(leafCodeElement).isEmpty() || leafCodeElement.isRemoved()) {
                    continue;
                }
                List<T> matched = new ArrayList<>();
                for (T rootCodeElement : rootCodeElements) {
                    if (!changeHistoryGraph.predecessors(rootCodeElement).isEmpty() && rootCodeElement.isAdded()) {
                        continue;
                    }
                    if (!rootCodeElement.getVersion().getId().equals(leafCodeElement.getVersion().getId())) {
                        matched.add(rootCodeElement);
                    }
                }
                if (!matched.isEmpty()) {
                    matched.sort(Comparator.comparingLong(o -> o.getVersion().getTime()));
                    changeHistoryGraph.putEdgeValue(leafCodeElement, matched.get(0), ChangeFactory.of(AbstractChange.Type.NO_CHANGE).asEdge());
                    rootCodeElements.remove(matched.get(0));
                }
            }
        }
    }

    public void handleRemoved(T leftSide, T rightSide) {
        if (leftSide == null || rightSide == null)
            return;
        changeHistoryGraph.addNode(leftSide);
        if (!changeHistoryGraph.successors(leftSide).isEmpty())
            return;
        rightSide.setRemoved(true);
        addChange(leftSide, rightSide, ChangeFactory.of(AbstractChange.Type.REMOVED).codeElement(leftSide));
    }

    public void handleAdd(T leftSide, T rightSide) {
        handleAdd(leftSide, rightSide, null);
    }

    public void handleAdd(T leftSide, T rightSide, String comment) {
        if (leftSide == null || rightSide == null)
            return;
        changeHistoryGraph.addNode(rightSide);
        if (!changeHistoryGraph.predecessors(rightSide).isEmpty())
            return;
        leftSide.setAdded(true);
        addChange(leftSide, rightSide, ChangeFactory.of(AbstractChange.Type.INTRODUCED).comment(comment).codeElement(rightSide));
    }

    public Graph<T, Edge> findSubGraph(T start) {
        return GraphImpl.subGraph(changeHistoryGraph, start);
    }

    public Set<T> predecessors(T codeElement) {
        return changeHistoryGraph.predecessors(codeElement);
    }

}
