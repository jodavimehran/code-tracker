package org.refactoringrefiner;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Edge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class RefactoringEdge implements Edge {
    private final String commitId;
    private final List<Refactoring> refactorings = new ArrayList<>();
    private boolean actualRefactoring;

    public RefactoringEdge(String commitId, Refactoring refactoring) {
        this.commitId = commitId;
        if (refactoring != null)
            this.refactorings.add(refactoring);
    }

    static Edge of(String commitId, Refactoring refactoring) {
        return new RefactoringEdge(commitId, refactoring);
    }

    @Override
    public String prettyPrint() {
        if (commitId == null || refactorings.isEmpty()) {
            return "";
        }
        return refactorings.stream()
                .map(refactoring -> refactoring.getRefactoringType().getDisplayName()).collect(Collectors.joining("|"));

    }

    @Override
    public Set<Refactoring> getRefactorings() {
        return new HashSet<>(refactorings);
    }

    @Override
    public String toString() {
        return refactorings.stream()
                .map(r -> r.getRefactoringType().getDisplayName()).collect(Collectors.joining(","));

    }

    @Override
    public void addRefactoring(Refactoring refactoring, boolean isActualRefactoring) {
        if (refactoring != null)
            refactorings.add(refactoring);
        if (!this.actualRefactoring) {
            this.actualRefactoring = isActualRefactoring;
        }
    }

    @Override
    public int numberOfRefactoring() {
        return refactorings.size();
    }

    @Override
    public String getCommitId() {
        return this.commitId;
    }

    @Override
    public boolean isActualRefactoring() {
        return actualRefactoring;
    }
}
