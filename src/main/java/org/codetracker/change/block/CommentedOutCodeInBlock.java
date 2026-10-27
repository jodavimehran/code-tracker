package org.codetracker.change.block;

public class CommentedOutCodeInBlock extends BlockBodyChange {
    public CommentedOutCodeInBlock() {
        super(Type.COMMENTED_OUT_STATEMENT);
    }

    @Override
    public String toString() {
        return "Body Change, Contains Commented-out Code";
    }

}
