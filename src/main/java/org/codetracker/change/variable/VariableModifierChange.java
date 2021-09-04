package org.codetracker.change.variable;

import org.refactoringminer.api.Refactoring;

public class VariableModifierChange extends VariableSignatureChange{
    public VariableModifierChange(Refactoring refactoring) {
        super(Type.MODIFIER_CHANGE, refactoring);
    }
}
