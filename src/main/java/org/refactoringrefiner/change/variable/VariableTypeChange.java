package org.refactoringrefiner.change.variable;

import org.refactoringminer.api.Refactoring;

public class VariableTypeChange extends VariableSignatureChange {
    public VariableTypeChange(Refactoring refactoring) {
        super(Type.TYPE_CHANGE, refactoring);
    }
}
