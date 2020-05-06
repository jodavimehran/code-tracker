package org.refactoringrefiner.api;

public interface Result {

    Graph<CodeElement, Edge> getGraph();

    MetaInfo getMetaInfo();
}
