package org.codetracker.change.clazz;

import org.refactoringminer.api.Refactoring;

public class ClassModifierChange extends ClassChange {
    public ClassModifierChange(Refactoring refactoring) {
        super(Type.MODIFIER_CHANGE, refactoring);
    }
}
