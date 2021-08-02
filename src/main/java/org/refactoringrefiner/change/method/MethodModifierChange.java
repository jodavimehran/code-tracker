package org.refactoringrefiner.change.method;

import org.refactoringminer.api.Refactoring;

public class MethodModifierChange extends SignatureChange{

    public MethodModifierChange(Refactoring refactoring) {
        super(Type.MODIFIER_CHANGE, refactoring);
    }

}
