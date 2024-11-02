package org.codetracker.change.block;

import org.codetracker.change.Change;

public class ElseBodyBlockAdded extends BlockChange {
	public ElseBodyBlockAdded() {
		super(Change.Type.ELSE_BODY_BLOCK_ADDED);
	}

    @Override
    public String toString() {
        return "Else Body Block Added";
    }
}
