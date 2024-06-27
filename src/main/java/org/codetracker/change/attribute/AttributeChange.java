package org.codetracker.change.attribute;

import org.codetracker.change.AbstractChange;
import org.refactoringminer.api.Refactoring;

public abstract class AttributeChange extends AbstractChange {
    private final Refactoring refactoring;

    public AttributeChange(Type type) {
    	super(type);
    	this.refactoring = null;
    }

    public AttributeChange(Type type, Refactoring refactoring) {
        super(type);
        this.refactoring = refactoring;
    }

    public final Refactoring getRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
    	if (refactoring != null)
    		return refactoring.toString();
    	return type.getTitle();
    }
}
