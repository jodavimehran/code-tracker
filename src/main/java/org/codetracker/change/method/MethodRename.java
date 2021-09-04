package org.codetracker.change.method;

import org.refactoringminer.api.Refactoring;

public class MethodRename extends MethodSignatureChange {
    public MethodRename(Refactoring refactoring) {
        super(Type.RENAME, refactoring);
    }

}
