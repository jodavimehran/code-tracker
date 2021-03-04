package org.refactoringrefiner;

import com.google.common.graph.*;
import org.refactoringminer.api.RefactoringType;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.api.Graph;

import java.util.*;

/**
 * @param <N> Node Type
 * @param <E> Edge Type
 */
public class GraphImpl<N extends CodeElement, E extends Edge> implements org.refactoringrefiner.api.Graph<N, E> {
    private static List<String> CTR = Arrays.asList(RefactoringType.CHANGE_ATTRIBUTE_TYPE.getDisplayName(),
            RefactoringType.CHANGE_PARAMETER_TYPE.getDisplayName(), RefactoringType.CHANGE_RETURN_TYPE.getDisplayName(), RefactoringType.CHANGE_VARIABLE_TYPE.getDisplayName());

    private MutableValueGraph<N, E> graph;

    /**
     * @param graph
     */
    private GraphImpl(ValueGraph<N, E> graph) {
        this.graph = Graphs.copyOf(graph);
    }

    public GraphImpl() {
        this.graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    }

    public static <N extends CodeElement, E extends Edge> GraphImpl<N, E> of(ValueGraph<N, E> graph) {
        return new GraphImpl<>(graph);
    }

    public static <N extends CodeElement, E extends Edge> Set<Graph<N, E>> setOf(ValueGraph<N, E> graph) {
        Set<Graph<N, E>> graphs = new HashSet<>();
        for (Set<N> nodes : induceDisconnectedSubGraphs(graph)) {
            graphs.add(new GraphImpl(Graphs.inducedSubgraph(graph, nodes)));
        }
        return graphs;
    }

    public static <N extends CodeElement, E extends Edge> Set<Graph<N, E>> setOf(Graph<N, E> graph) {
        Set<Graph<N, E>> graphs = new HashSet<>();
        for (Set<N> nodes : induceDisconnectedSubGraphs(((GraphImpl<N, E>) graph).graph)) {
            graphs.add(new GraphImpl(Graphs.inducedSubgraph(((GraphImpl) graph).graph, nodes)));
        }
        return graphs;
    }

    public static <N extends CodeElement, E extends Edge> List<Set<N>> induceDisconnectedSubGraphs(ValueGraph<N, E> graph) {
        List<Set<N>> inducedSubGraphNodes = new ArrayList<>();
        HashMap<N, Set<N>> visitedNodes = new HashMap<>();
        for (N node : graph.nodes()) {
            if (graph.predecessors(node).isEmpty()) {
                if (!visitedNodes.containsKey(node)) {
                    Set<N> reachable = new HashSet<>(findReachable(graph, node));
                    reachable.add(node);

                    Set<N> relatedReachable = null;
                    for (N reachableNode : reachable) {
                        if (visitedNodes.containsKey(reachableNode)) {
                            relatedReachable = visitedNodes.get(reachableNode);
                            break;
                        }
                    }
                    if (relatedReachable == null) {
                        inducedSubGraphNodes.add(reachable);
                        relatedReachable = reachable;
                    }
                    for (N reachableNode : reachable) {
                        visitedNodes.put(reachableNode, relatedReachable);
                    }
                }
            }
        }
        return inducedSubGraphNodes;
    }

    private static <N extends CodeElement, E extends Edge> Set<N> findReachable(ValueGraph<N, E> graph, N node) {
        Set<N> reachable = new HashSet<>();
        Traverser.forGraph(graph).breadthFirst(node).forEach(reachable::add);
        return reachable;
    }

//    public static <N extends CodeElement, E extends Edge> Map<N, E> getEdgesContainingCTR(GraphImpl<N, E> graph) {
//        return graph.graph.edges().stream()
//                .filter(x -> graph.graph.edgeValue(x).get().numberOfRefactoring() > 1)
//                .filter(x -> Arrays.stream(graph.graph.edgeValue(x).get().toString().split(",")).anyMatch(r -> CTR.contains(r)))
//                .collect(toMap(x -> x.nodeU(), x -> graph.graph.edgeValue(x).get(), (e1, e2) -> e1));
//    }

    public void merge(GraphImpl<N, E> toMergeGraph) {
        for (EndpointPair<N> edge : toMergeGraph.graph.edges()) {
            E e = toMergeGraph.graph.edgeValueOrDefault(edge, null);
            if (e != null)
                graph.putEdgeValue(edge, e);
        }
    }

    @Override
    public Set<N> getNodeList() {
        return graph.nodes();
    }

    @Override
    public Set<EndpointPair<N>> getEdges() {
        return graph.edges();
    }

    @Override
    public Optional<E> getEdgeValue(EndpointPair<N> e) {
        return graph.edgeValue(e);
    }

    /**
     * @return true if the chain is empty
     */
    public boolean isEmpty() {
        return graph.nodes().isEmpty();
    }

    @Override
    public Set<N> predecessors(N n) {
        return graph.predecessors(n);
    }

    @Override
    public Set<N> successors(N n) {
        return graph.successors(n);
    }

//    public boolean isMultiContainer() {
//        return graph.nodes().stream().map(n -> n.getContainerName()).collect(Collectors.toSet()).size() > 1;
//    }
}
