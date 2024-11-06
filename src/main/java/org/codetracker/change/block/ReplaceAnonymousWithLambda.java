package org.codetracker.change.block;

import org.refactoringminer.api.Refactoring;

public class ReplaceAnonymousWithLambda extends BlockChange {
    private final Refactoring refactoring;

    public ReplaceAnonymousWithLambda(Refactoring refactoring) {
        super(Type.REPLACE_ANONYMOUS_WITH_LAMBDA);
        this.refactoring = refactoring;
    }

    public Refactoring getReplaceAnonymousWithLambdaRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        return refactoring.toString();
    }
}
