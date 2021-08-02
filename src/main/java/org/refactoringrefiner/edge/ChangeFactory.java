package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.change.AbstractChange;
import org.refactoringrefiner.change.method.*;

public final class ChangeFactory {
    protected final AbstractChange.Type type;
    protected final String elementType;
    private Refactoring refactoring;
    private Refactoring relatedRefactoring;
    private CodeElement codeElement;

    private ChangeFactory(AbstractChange.Type type, String elementType) {
        this.type = type;
        this.elementType = elementType;
    }

    public static ChangeFactory forMethod(AbstractChange.Type type) {
        return new ChangeFactory(type, "method");
    }

    public static ChangeFactory of(AbstractChange.Type type) {
        return new ChangeFactory(type, null);
    }

    public ChangeFactory refactoring(Refactoring refactoring) {
        this.refactoring = refactoring;
        return this;
    }

    public boolean containsRefactoring(){
        return this.refactoring != null;
    }

    public ChangeFactory relatedRefactoring(Refactoring relatedRefactoring) {
        this.relatedRefactoring = relatedRefactoring;
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

        AbstractChange change = null;
        switch (type) {
            case NO_CHANGE:
                change = new NoChange();
                break;

            case CONTAINER_CHANGE:
                change = new ContainerChange(refactoring);
                break;
            case INTRODUCED: {
                if (codeElement == null)
                    throw new NullPointerException();
                if (refactoring != null)
                    change = new Extracted(refactoring, codeElement);
                else
                    change = new Added(codeElement);
                break;
            }
            case REMOVED:
                change = new Removed(codeElement);
                break;
            case DOCUMENTATION_CHANGE:
                change = new DocumentationChange();
                break;
            case BODY_CHANGE:
                change = new BodyChange();
                break;
            case RETURN_TYPE_CHANGE: {
                if (refactoring == null)
                    throw new NullPointerException();
                change = new ReturnTypeChange(refactoring);
                break;
            }
            case RENAME: {
                if (refactoring == null)
                    throw new NullPointerException();
                if ("method".equals(elementType)) {
                    change = new MethodRename(refactoring);
                }
                break;
            }
            case METHOD_MOVE: {
                if (refactoring == null)
                    throw new NullPointerException();
                if ("method".equals(elementType)) {
                    change = new MethodMove(refactoring);
                }
                break;
            }
            case MODIFIER_CHANGE: {
                if (refactoring == null)
                    throw new NullPointerException();
                if ("method".equals(elementType)) {
                    change = new MethodModifierChange(refactoring);
                }
                break;
            }
            case EXCEPTION_CHANGE: {
                if (refactoring == null)
                    throw new NullPointerException();
                change = new ExceptionChange(refactoring);
                break;
            }
            case PARAMETER_CHANGE: {
                if (refactoring == null)
                    throw new NullPointerException();
                change = new ParameterChange(refactoring);
                break;
            }
            case ANNOTATION_CHANGE: {
                if (refactoring == null)
                    throw new NullPointerException();
                if ("method".equals(elementType)) {
                    change = new AnnotationChange(refactoring);
                }
                break;
            }

            default:
                return null;
        }

        return change;
    }
}
