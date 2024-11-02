package org.codetracker.change.block;

import org.codetracker.change.Change;

public class ElseBodyBlockRemoved extends BlockChange {
	public ElseBodyBlockRemoved() {
		super(Change.Type.ELSE_BODY_BLOCK_REMOVED);
	}

    @Override
    public String toString() {
        return "Else Body Block Removed";
    }
}
