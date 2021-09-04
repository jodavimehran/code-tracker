package org.codetracker.change.variable;


import org.refactoringminer.api.Refactoring;

public class VariableRename extends VariableSignatureChange {
    public VariableRename(Refactoring refactoring) {
        super(Type.RENAME, refactoring);
    }
}
