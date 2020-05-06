package org.refactoringrefiner;

import org.refactoringrefiner.api.*;

public class ResultImpl implements Result {
    private final Graph<CodeElement, Edge> graph;
    private final MetaInfo metaInfo;

    public ResultImpl(GraphImpl<CodeElement, Edge> graph, MetaInfo metaInfo) {
        this.graph = graph;
        this.metaInfo = metaInfo;
    }

    @Override
    public Graph<CodeElement, Edge> getGraph() {
        return graph;
    }

    @Override
    public MetaInfo getMetaInfo() {
        return metaInfo;
    }
}
