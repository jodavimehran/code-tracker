package org.codetracker.change.clazz;

import org.refactoringminer.api.Refactoring;

public class ClassMove extends ClassChange {
    public ClassMove(Refactoring refactoring) {
        super(Type.MOVED, refactoring);
    }
}
