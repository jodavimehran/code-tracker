package org.refactoringrefiner.api;

import org.refactoringminer.api.Refactoring;

import java.util.List;

public interface Result {

    List<Refactoring> getAggregatedRefactorings();

    List<Refactoring> getRefactorings();

    int getCommitCount();

    Graph<CodeElement, Edge> getAttributeChangeHistoryGraph();

    Graph<CodeElement, Edge> getClassChangeHistoryGraph();

    Graph<CodeElement, Edge> getMethodChangeHistoryGraph();

    Graph<CodeElement, Edge> getVariableChangeHistoryGraph();
}
