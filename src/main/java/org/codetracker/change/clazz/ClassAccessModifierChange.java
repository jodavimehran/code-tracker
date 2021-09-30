package org.codetracker.change.clazz;

import org.refactoringminer.api.Refactoring;

public class ClassAccessModifierChange extends ClassChange {
    public ClassAccessModifierChange(Refactoring refactoring) {
        super(Type.ACCESS_MODIFIER_CHANGE, refactoring);
    }
}
