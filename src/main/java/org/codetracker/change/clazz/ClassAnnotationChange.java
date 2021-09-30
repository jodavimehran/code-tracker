package org.codetracker.change.clazz;

import org.refactoringminer.api.Refactoring;

public class ClassAnnotationChange extends ClassChange {

    public ClassAnnotationChange(Refactoring refactoring) {
        super(Type.ANNOTATION_CHANGE, refactoring);
    }
}
