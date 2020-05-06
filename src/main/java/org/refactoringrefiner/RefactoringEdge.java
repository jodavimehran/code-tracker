package org.refactoringrefiner;

import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Edge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class RefactoringEdge implements Edge {
    private final RevCommit commit;
    private final List<Refactoring> refactorings = new ArrayList<>();
    public RefactoringEdge(RevCommit revCommit, Refactoring refactoring) {
        this.commit = revCommit;
        if (refactoring != null)
            this.refactorings.add(refactoring);
    }

    static Edge of(RevCommit commit, Refactoring refactoring) {
        return new RefactoringEdge(commit, refactoring);
    }

    @Override
    public String prettyPrint() {
        if (commit == null || refactorings.isEmpty()) {
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

    public void addRefactoring(Refactoring refactoring) {
        if (refactoring != null)
            refactorings.add(refactoring);
    }

    @Override
    public int numberOfRefactoring() {
        return refactorings.size();
    }

    @Override
    public int getCommitTime() {
        return this.commit != null ? this.commit.getCommitTime() : -1;
    }

    @Override
    public String getCommitId() {
        return this.commit != null ? this.commit.getId().getName() : "";
    }
}
