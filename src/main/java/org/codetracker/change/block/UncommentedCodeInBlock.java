package org.codetracker.change.block;

import org.codetracker.change.Change;

public class UncommentedCodeInBlock extends BlockBodyChange {
    public UncommentedCodeInBlock() {
        super(Change.Type.UNCOMMENTED_STATEMENT);
    }

    @Override
    public String toString() {
        return "Body Change, Contains Uncommented Code";
    }

}
