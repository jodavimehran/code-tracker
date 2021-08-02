package org.refactoringrefiner.change.method;

import org.refactoringminer.api.Refactoring;

public class MethodRename extends SignatureChange {
    public MethodRename(Refactoring refactoring) {
        super(Type.RENAME, refactoring);
    }

}
