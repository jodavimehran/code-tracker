package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;

public final class ChangeFactory {
    protected final AbstractChange.Type type;
    private Refactoring refactoring;
    private Refactoring relatedRefactoring;
    private CodeElement codeElement;
    private String description;

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

    public ChangeFactory relatedRefactoring(Refactoring relatedRefactoring) {
        this.relatedRefactoring = relatedRefactoring;
        return this;
    }

    public ChangeFactory codeElement(CodeElement codeElement) {
        this.codeElement = codeElement;
        return this;
    }

    public ChangeFactory description(String description) {
        this.description = description;
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

        if (description == null && refactoring != null) {
            description = refactoring.toString();
        }

        AbstractChange change;
        switch (type) {
            case INLINED:
                if (refactoring == null && description == null)
                    throw new NullPointerException();
                change = new Inlined(refactoring, codeElement, description);
                break;
            case REFACTORED:
                if (refactoring == null && description == null)
                    throw new NullPointerException();
                if (relatedRefactoring != null)
                    change = new Refactored(refactoring, relatedRefactoring, description);
                else
                    change = new Refactored(refactoring, description);
                break;
            case EXTRACTED:
                if (refactoring == null && description == null)
                    throw new NullPointerException();
                change = new Extracted(refactoring, codeElement, description);
                break;
            case CONTAINER_CHANGE:
                if (refactoring == null && description == null)
                    throw new NullPointerException();
                change = new ContainerChange(refactoring, description);
                break;
            case ADDED:
                change = new Added(codeElement, description);
                break;
            case REMOVED:
                change = new Removed(codeElement, description);
                break;
            case MODIFIED:
                change = new Modified(refactoring, description);
                break;
            case NO_CHANGE:
                change = new NoChange();
                break;
            case BRANCHED:
                if (refactoring == null && description == null)
                    throw new NullPointerException();
                change = new Branched(refactoring, description);
                break;
            case MERGED:
                if (refactoring == null && description == null)
                    throw new NullPointerException();
                change = new Merged(refactoring, description);
                break;
            default:
                return null;
        }

        return change;
    }
}
