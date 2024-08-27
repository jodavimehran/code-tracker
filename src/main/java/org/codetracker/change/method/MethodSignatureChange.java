package org.codetracker.change.method;

import org.refactoringminer.api.Refactoring;

public abstract class MethodSignatureChange extends MethodChange {
    private final Refactoring refactoring;

    public MethodSignatureChange(Type type) {
        super(type);
        this.refactoring = null;
    }

    public MethodSignatureChange(Type type, Refactoring refactoring) {
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
    	return "";
    }
}
