package org.codetracker.change.attribute;

import org.refactoringminer.api.Refactoring;

public class AttributeRename extends AttributeChange {
    public AttributeRename(Refactoring refactoring) {
        super(Type.RENAME, refactoring);
    }
}
