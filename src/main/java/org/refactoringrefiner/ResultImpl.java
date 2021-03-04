package org.refactoringrefiner;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.api.Result;
import org.refactoringrefiner.edge.EdgeImpl;
import org.refactoringrefiner.element.Attribute;
import org.refactoringrefiner.element.Class;
import org.refactoringrefiner.element.Method;
import org.refactoringrefiner.element.Variable;

import java.util.*;
import java.util.stream.Collectors;

public class ResultImpl implements Result {
    private final GraphImpl<CodeElement, Edge> attributeChangeHistoryGraph;
    private final GraphImpl<CodeElement, Edge> classChangeHistoryGraph;
    private final GraphImpl<CodeElement, Edge> methodChangeHistoryGraph;
    private final GraphImpl<CodeElement, Edge> variableChangeHistoryGraph;
    private final RefactoringHandlerImpl refactoringHandler;
    private final List<Refactoring> aggregatedRefactorings;
    private double sameCodeElementChangeRate;
    private long refactoringMinerProcessTime, refactoringRefinerProcessTime;

    public ResultImpl(RefactoringHandlerImpl refactoringHandler) {
        this.refactoringHandler = refactoringHandler;
        this.aggregatedRefactorings = findRefactorings();
        attributeChangeHistoryGraph = GraphImpl.of(refactoringHandler.getAttributeChangeHistoryGraph());
        classChangeHistoryGraph = GraphImpl.of(refactoringHandler.getClassChangeHistoryGraph());
        methodChangeHistoryGraph = GraphImpl.of(refactoringHandler.getMethodChangeHistoryGraph());
        variableChangeHistoryGraph = GraphImpl.of(refactoringHandler.getVariableChangeHistoryGraph());
    }

    private static HashSet<EndpointPair<CodeElement>> getPair(ImmutableValueGraph<CodeElement, Edge> graph) {
        HashSet<EndpointPair<CodeElement>> pairsSet = new HashSet<>();

        Set<EndpointPair<CodeElement>> analyzed = new HashSet<>();
        for (EndpointPair<CodeElement> endpointPair : graph.edges()) {
            EdgeImpl edge = (EdgeImpl) graph.edgeValue(endpointPair).get();
            if (analyzed.contains(endpointPair)) {
                continue;
            }
            analyzed.add(endpointPair);
            switch (edge.getType()) {
                case INLINED:
                case EXTRACTED: {
//                    CodeElement mostLeftSideSource = findMostLeftSide(endpointPair.source());
//                    CodeElement mostRightSideSource = findMostRightSide(endpointPair.source());
//
//                    CodeElement mostLeftSideTarget = findMostLeftSide(endpointPair.target());
//                    CodeElement mostRightSideTarget = findMostRightSide(endpointPair.target());
//
//                    if (mostLeftSideSource instanceof Method) {
//                        Refactoring refactoring = null;
//                        if (edge instanceof Inlined) {
//                            refactoring = new Method.InlineRefactoringBuilder((Method) mostLeftSideSource, (Method) mostLeftSideTarget, (Method) mostRightSideTarget).getRefactoring();
//                        } else if (edge instanceof Extracted) {
//                            refactoring = new Method.ExtractRefactoringBuilder((Method) mostRightSideTarget, (Method) mostLeftSideSource, (Method) mostRightSideSource).getRefactoring();
//                        }
//                        if (refactoring != null) {
//                            result.add(refactoring);
//                        }
//                    }
                    break;
                }
                default:
                    if (!graph.predecessors(endpointPair.source()).isEmpty()) {
                        continue;
                    }
                    List<CodeElement> mostLeftSideList = findMostLeftSide(endpointPair.source(), graph, analyzed);
                    List<CodeElement> mostRightSideList = findMostRightSide(endpointPair.target(), graph, analyzed);
                    for (CodeElement mostLeftSide : mostLeftSideList) {
                        for (CodeElement mostRightSide : mostRightSideList) {
                            if (mostLeftSide.isAdded() || mostRightSide.isRemoved()) {
                                continue;
                            }
                            pairsSet.add(EndpointPair.ordered(mostLeftSide, mostRightSide));
                        }
                    }
                    break;
            }
        }
        return pairsSet;
    }

    private static List<CodeElement> findMostRightSide(CodeElement codeElement, ImmutableValueGraph<CodeElement, Edge> graph, Set<EndpointPair<CodeElement>> analyzed) {
        List<CodeElement> codeElementList = new ArrayList<>();
        Set<CodeElement> successors = graph.successors(codeElement);
        if (successors.isEmpty()) {
            codeElementList.add(codeElement);
            return codeElementList;
        }

        for (CodeElement rightElement : successors) {
            EndpointPair<CodeElement> endpointPair = EndpointPair.ordered(codeElement, rightElement);
            if (!analyzed.contains(endpointPair)) {
                analyzed.add(endpointPair);
                EdgeImpl edge = (EdgeImpl) graph.edgeValue(codeElement, rightElement).get();

                codeElementList.addAll(findMostRightSide(rightElement, graph, analyzed));

            }
        }
        return codeElementList;
    }

    private static List<CodeElement> findMostLeftSide(CodeElement codeElement, ImmutableValueGraph<CodeElement, Edge> graph, Set<EndpointPair<CodeElement>> analyzed) {
        List<CodeElement> codeElementList = new ArrayList<>();
        Set<CodeElement> predecessors = graph.predecessors(codeElement);
        if (predecessors.isEmpty()) {
            codeElementList.add(codeElement);
            return codeElementList;
        }
        for (CodeElement leftElement : predecessors) {
            EndpointPair<CodeElement> endpointPair = EndpointPair.ordered(leftElement, codeElement);
            if (!analyzed.contains(endpointPair)) {
                analyzed.add(endpointPair);
                EdgeImpl edge = (EdgeImpl) graph.edgeValue(leftElement, codeElement).get();

                codeElementList.addAll(findMostLeftSide(leftElement, graph, analyzed));

            }
        }
        return codeElementList;
    }

    @Override
    public List<Refactoring> getRefactorings() {
        return refactoringHandler.getRefactorings();
    }

    @Override
    public List<Refactoring> getAggregatedRefactorings() {
        return aggregatedRefactorings;
    }

    @Override
    public int getCommitCount() {
        return refactoringHandler.getCommitsCount();
    }

    @Override
    public GraphImpl<CodeElement, Edge> getAttributeChangeHistoryGraph() {
        return attributeChangeHistoryGraph;
    }

    @Override
    public GraphImpl<CodeElement, Edge> getClassChangeHistoryGraph() {
        return classChangeHistoryGraph;
    }

    @Override
    public GraphImpl<CodeElement, Edge> getMethodChangeHistoryGraph() {
        return methodChangeHistoryGraph;
    }

    @Override
    public GraphImpl<CodeElement, Edge> getVariableChangeHistoryGraph() {
        return variableChangeHistoryGraph;
    }

    public long getRefactoringMinerProcessTime() {
        return refactoringMinerProcessTime;
    }

    public void setRefactoringMinerProcessTime(long refactoringMinerProcessTime) {
        this.refactoringMinerProcessTime = refactoringMinerProcessTime;
    }

    public long getRefactoringRefinerProcessTime() {
        return refactoringRefinerProcessTime;
    }

    public void setRefactoringRefinerProcessTime(long refactoringRefinerProcessTime) {
        this.refactoringRefinerProcessTime = refactoringRefinerProcessTime;
    }

    public double getSameCodeElementChangeRate() {
        return sameCodeElementChangeRate;
    }

    public List<Refactoring> findRefactorings() {
        HashSet<EndpointPair<CodeElement>> classLevelPairs = getPair(refactoringHandler.getClassChangeHistoryGraph());
        HashSet<EndpointPair<CodeElement>> methodLevelPairs = getPair(refactoringHandler.getMethodChangeHistoryGraph());
        HashSet<EndpointPair<CodeElement>> attributeLevelPairs = getPair(refactoringHandler.getAttributeChangeHistoryGraph());
        HashSet<EndpointPair<CodeElement>> variableLevelPairs = getPair(refactoringHandler.getVariableChangeHistoryGraph());

        HashMap<String, String> renamedOrMovedClasses = new HashMap<>();
        List<Refactoring> result = new ArrayList<>();


        HashMap<String, MoveSourceFolderRefactoring> moveSourceFolderMap = new HashMap<>();
        result.addAll(
                classLevelPairs.stream()
                        .flatMap(pair -> new Class.ClassElementDiff((Class) pair.source(), (Class) pair.target()).getRefactorings(moveSourceFolderMap).stream())
                        .collect(Collectors.toSet())
        );

        classLevelPairs.stream().forEach(classElements -> renamedOrMovedClasses.put(classElements.source().getFullName(), classElements.target().getFullName()));

        result.addAll(
                methodLevelPairs.stream()
                        .flatMap(pair -> new Method.MethodElementDiff((Method) pair.source(), (Method) pair.target()).getRefactorings(renamedOrMovedClasses).stream())
                        .collect(Collectors.toSet())
        );

        result.addAll(
                attributeLevelPairs.stream()
                        .flatMap(pair -> new Attribute.AttributeElementDiff((Attribute) pair.source(), (Attribute) pair.target()).getRefactorings(renamedOrMovedClasses).stream())
                        .collect(Collectors.toSet())
        );

        result.addAll(
                variableLevelPairs.stream()
                        .flatMap(pair -> new Variable.VariableElementDiff((Variable) pair.source(), (Variable) pair.target()).getRefactorings().stream())
                        .collect(Collectors.toSet())
        );

        sameCodeElementChangeRate = ((double) classLevelPairs.size() + methodLevelPairs.size() + attributeLevelPairs.size() + variableLevelPairs.size()) / refactoringHandler.getNumberOfEdge();
        return result;
    }
}
