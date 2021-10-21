package org.codetracker.change.clazz;

import org.refactoringminer.api.Refactoring;

public class ClassDeclarationKindChange extends ClassChange {
    public ClassDeclarationKindChange(Refactoring refactoring) {
        super(Type.TYPE_CHANGE, refactoring);
    }
}
