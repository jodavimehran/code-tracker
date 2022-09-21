package org.codetracker.change.block;

import org.codetracker.change.Change;

public class CatchBlockAdded extends BlockChange {
    public CatchBlockAdded() {
        super(Change.Type.CATCH_BLOCK_ADDED);
    }

    @Override
    public String toString() {
        return "Catch Block Added";
    }
}
