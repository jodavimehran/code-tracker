package org.refactoringrefiner.api;

import com.google.common.graph.EndpointPair;

import java.util.Optional;
import java.util.Set;

public interface Graph<N extends CodeElement, E extends Edge> {

    Set<N> getNodeList();

    Set<EndpointPair<N>> getEdges();

    Optional<E> getEdgeValue(EndpointPair<N> e);

    Set<N> predecessors(N n);

    Set<N> successors(N n);
}
