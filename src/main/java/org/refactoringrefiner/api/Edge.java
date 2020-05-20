package org.refactoringrefiner.api;

import org.refactoringminer.api.Refactoring;

import java.util.Set;

public interface Edge {

    void addRefactoring(Refactoring refactoring);

    int numberOfRefactoring();

    String prettyPrint();

    Set<Refactoring> getRefactorings();

    String getCommitId();
}
