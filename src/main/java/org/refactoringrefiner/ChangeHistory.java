package org.refactoringrefiner;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Change;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.api.Graph;
import org.refactoringrefiner.edge.AbstractChange;
import org.refactoringrefiner.edge.ChangeFactory;
import org.refactoringrefiner.edge.EdgeImpl;
import org.refactoringrefiner.element.BaseCodeElement;

import java.util.*;

public class ChangeHistory {
    private final MutableValueGraph<CodeElement, Edge> changeHistoryGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    private final HashMap<String, Set<CodeElement>> elementsMapByName = new HashMap<>();
    private final HashMap<String, CodeElement> elementsByIdentifier = new HashMap<>();

    private static void addCodeElementToMap(String name, CodeElement codeElement, HashMap<String, Set<CodeElement>> elementsMap) {
        Set<CodeElement> codeElements;
        if (elementsMap.containsKey(name)) {
            codeElements = elementsMap.get(name);
        } else {
            codeElements = new HashSet<>();
            elementsMap.put(name, codeElements);
        }
        codeElements.add(codeElement);
    }

    public void addChange(CodeElement leftSide, CodeElement rightSide, ChangeFactory changeFactory) {
        if (leftSide == null || rightSide == null)
            return;
        if (leftSide.equals(rightSide))
            return;
        Optional<Edge> edgeValue = changeHistoryGraph.edgeValue(leftSide, rightSide);
        if (edgeValue.isPresent()) {
            EdgeImpl edge = (EdgeImpl) edgeValue.get();
            edge.addChange(changeFactory.build());
        } else {
            changeHistoryGraph.putEdgeValue(leftSide, rightSide, changeFactory.asEdge());
        }
    }

    public int getNumberOfEdge() {
        return changeHistoryGraph.edges().size();
    }

    public CodeElement getCodeElementByIdentifier(String identifier) {
        return elementsByIdentifier.getOrDefault(identifier, null);
    }

    public CodeElement addNode(CodeElement codeElement) {
        if (elementsByIdentifier.containsKey(codeElement.getIdentifier())) {
            return elementsByIdentifier.get(codeElement.getIdentifier());
        }
        elementsByIdentifier.put(codeElement.getIdentifier(), codeElement);
        elementsMapByName.putIfAbsent(codeElement.getName(), new HashSet<>());
        elementsMapByName.get(codeElement.getName()).add(codeElement);
        changeHistoryGraph.addNode(codeElement);
        return codeElement;
    }

    public void connectRelatedNodes() {
        HashMap<String, Set<CodeElement>> leafElementsByIdentifier = new HashMap<>();
        HashMap<String, Set<CodeElement>> rootElementByIdentifier = new HashMap<>();

        HashMap<String, Set<CodeElement>> leafElementsByName = new HashMap<>();
        HashMap<String, Set<CodeElement>> rootElementByName = new HashMap<>();
        for (CodeElement node : changeHistoryGraph.nodes()) {
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

    private void matchElements(HashMap<String, Set<CodeElement>> leafElements, HashMap<String, Set<CodeElement>> rootElement) {
        for (Map.Entry<String, Set<CodeElement>> leafEntry : leafElements.entrySet()) {
            if (!rootElement.containsKey(leafEntry.getKey())) {
                continue;
            }
            List<CodeElement> leafCodeElementsList = new ArrayList<>(leafEntry.getValue());
            leafCodeElementsList.sort((o1, o2) -> Long.compare(o2.getVersion().getTime(), o1.getVersion().getTime()));
            Set<CodeElement> rootCodeElements = rootElement.get(leafEntry.getKey());
            for (CodeElement leafCodeElement : leafCodeElementsList) {
                if (!changeHistoryGraph.successors(leafCodeElement).isEmpty() || leafCodeElement.isRemoved()) {
                    continue;
                }
                List<CodeElement> matched = new ArrayList<>();
                for (CodeElement rootCodeElement : rootCodeElements) {
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

    public void handleRemoved(BaseCodeElement leftSide, BaseCodeElement rightSide) {
        handleRemoved(leftSide, rightSide, null);
    }

    public void handleRemoved(BaseCodeElement leftSide, BaseCodeElement rightSide, String description) {
        if (leftSide == null || rightSide == null)
            return;
        if (!changeHistoryGraph.successors(leftSide).isEmpty())
            return;
        rightSide.setRemoved(true);
        addChange(leftSide, rightSide, ChangeFactory.of(AbstractChange.Type.REMOVED).codeElement(leftSide).description(description));
    }

    public void handleAdd(BaseCodeElement leftSide, BaseCodeElement rightSide) {
        handleAdd(leftSide, rightSide, null);
    }

    public void handleAdd(BaseCodeElement leftSide, BaseCodeElement rightSide, String description) {
        if (leftSide == null || rightSide == null)
            return;
        if (!changeHistoryGraph.predecessors(rightSide).isEmpty())
            return;
        leftSide.setAdded(true);
        addChange(leftSide, rightSide, ChangeFactory.of(AbstractChange.Type.ADDED).codeElement(rightSide).description(description));
    }

    public List<CodeElement> findMostLeftSide(String codeElementName) {
        if (elementsMapByName.containsKey(codeElementName) && !elementsMapByName.get(codeElementName).isEmpty())
            return findMostLeftSide(elementsMapByName.get(codeElementName).iterator().next(), new HashSet<>());
        return Collections.emptyList();
    }

    private List<CodeElement> findMostLeftSide(CodeElement codeElement, Set<EndpointPair<CodeElement>> analyzed) {
        List<CodeElement> codeElementList = new ArrayList<>();
        Set<CodeElement> predecessors = changeHistoryGraph.predecessors(codeElement);
        if (predecessors.isEmpty() || codeElement.isAdded()) {
            codeElementList.add(codeElement);
            return codeElementList;
        }
        for (CodeElement leftElement : predecessors) {
            EndpointPair<CodeElement> endpointPair = EndpointPair.ordered(leftElement, codeElement);
            Change.Type type = changeHistoryGraph.edgeValue(endpointPair).get().getType();
            if (Change.Type.BRANCHED.equals(type) || Change.Type.MERGED.equals(type))
                continue;
            if (!analyzed.contains(endpointPair)) {
                analyzed.add(endpointPair);
                codeElementList.addAll(findMostLeftSide(leftElement, analyzed));
            }
        }
        return codeElementList;
    }

    public Graph<CodeElement, Edge> findSubGraph(CodeElement start) {
        return GraphImpl.subGraph(changeHistoryGraph, start);
    }

    public Set<CodeElement> predecessors(CodeElement codeElement){
        return changeHistoryGraph.predecessors(codeElement);
    }

    public void addRefactored(CodeElement leftSide, CodeElement rightSide, Refactoring refactoring) {
        addRefactored(leftSide, rightSide, refactoring, null);
    }
    public void addRefactored(CodeElement leftSide, CodeElement rightSide, Refactoring refactoring, Refactoring relatedRefactoring) {
        addChange(leftSide, rightSide, ChangeFactory.of(AbstractChange.Type.REFACTORED).refactoring(refactoring).relatedRefactoring(relatedRefactoring).description(refactoring.toString()));
    }
}
