package org.codetracker.change.attribute;

import org.refactoringminer.api.Refactoring;

public class AttributeAnnotationChange extends AttributeChange {

    public AttributeAnnotationChange(Refactoring refactoring) {
        super(Type.ANNOTATION_CHANGE, refactoring);
    }
}
