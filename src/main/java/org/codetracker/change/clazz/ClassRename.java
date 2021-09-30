package org.codetracker.change.clazz;

import org.refactoringminer.api.Refactoring;

public class ClassRename extends ClassChange {
    public ClassRename(Refactoring refactoring) {
        super(Type.RENAME, refactoring);
    }
}
