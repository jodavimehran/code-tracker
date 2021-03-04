package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;

public final class ChangeFactory {
    protected final AbstractChange.Type type;
    private Refactoring refactoring;
    private CodeElement codeElement;

    private ChangeFactory(AbstractChange.Type type) {
        this.type = type;
    }

    public static ChangeFactory of(AbstractChange.Type type) {
        return new ChangeFactory(type);
    }

    public ChangeFactory refactoring(Refactoring refactoring) {
        this.refactoring = refactoring;
        return this;
    }

    public ChangeFactory codeElement(CodeElement codeElement) {
        this.codeElement = codeElement;
        return this;
    }

    public AbstractChange.Type getType() {
        return type;
    }

    public Edge asEdge() {
        EdgeImpl edge = new EdgeImpl();
        AbstractChange change = build();
        if (change != null)
            edge.addChange(change);

        return edge;
    }

    public AbstractChange build() {
        if (type == null)
            return null;

        AbstractChange change;
        switch (type) {
            case INLINED:
                if (refactoring == null)
                    throw new NullPointerException();
                change = new Inlined(refactoring, codeElement);
                break;
            case REFACTORED:
                if (refactoring == null)
                    throw new NullPointerException();
                change = new Refactored(refactoring);
                break;
            case EXTRACTED:
                if (refactoring == null)
                    throw new NullPointerException();
                change = new Extracted(refactoring, codeElement);
                break;
            case CONTAINER_CHANGE:
                if (refactoring == null)
                    throw new NullPointerException();
                change = new ContainerChange(refactoring);
                break;
            case ADDED:
                change = new Added(codeElement);
                break;
            case REMOVED:
                change = new Removed(codeElement);
                break;
            case MODIFIED:
                change = new Modified(refactoring);
                break;
            case NO_CHANGE:
                change = new NoChange();
                break;

            default:
                return null;
        }

        return change;
    }
}
