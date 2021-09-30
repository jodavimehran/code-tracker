package org.codetracker.change.method;

import org.refactoringminer.api.Refactoring;

public class MethodAccessModifierChange extends MethodSignatureChange {

    public MethodAccessModifierChange(Refactoring refactoring) {
        super(Type.ACCESS_MODIFIER_CHANGE, refactoring);
    }

}
