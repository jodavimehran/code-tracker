package org.codetracker.change.block;

public class UncommentedBlock extends BlockChange {
    public UncommentedBlock() {
        super(Type.UNCOMMENTED_BLOCK);
    }

    @Override
    public String toString() {
        return "Block Change, Uncommented Block";
    }

}
