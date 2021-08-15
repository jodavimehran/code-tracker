package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Change;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.change.AbstractChange;
import org.refactoringrefiner.change.method.*;
import org.refactoringrefiner.change.variable.VariableAnnotationChange;
import org.refactoringrefiner.change.variable.VariableContainerChange;
import org.refactoringrefiner.change.variable.VariableModifierChange;
import org.refactoringrefiner.change.variable.VariableRename;

public final class ChangeFactory {
    protected final Change.Type type;
    protected final String elementType;
    private Refactoring refactoring;
    private Refactoring relatedRefactoring;
    private CodeElement codeElement;

    private ChangeFactory(Change.Type type, String elementType) {
        this.type = type;
        this.elementType = elementType;
    }

    public static ChangeFactory forMethod(Change.Type type) {
        return new ChangeFactory(type, "method");
    }

    public static ChangeFactory forVariable(Change.Type type) {
        return new ChangeFactory(type, "variable");
    }

    public static ChangeFactory of(Change.Type type) {
        return new ChangeFactory(type, null);
    }

    public ChangeFactory refactoring(Refactoring refactoring) {
        this.refactoring = refactoring;
        return this;
    }

    public boolean containsRefactoring() {
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

    public Change.Type getType() {
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
                if(isMethod()){
                    change = new MethodContainerChange(refactoring);
                }else if(isVariable()){
                    change = new VariableContainerChange(refactoring);
                }
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
                if (isMethod()) {
                    change = new MethodRename(refactoring);
                } else if (isVariable()) {
                    change = new VariableRename(refactoring);
                }
                break;
            }
            case METHOD_MOVE: {
                if (refactoring == null)
                    throw new NullPointerException();
                if (isMethod()) {
                    change = new MethodMove(refactoring);
                }
                break;
            }
            case MODIFIER_CHANGE: {
                if (refactoring == null)
                    throw new NullPointerException();
                if (isMethod()) {
                    change = new MethodModifierChange(refactoring);
                } else if (isVariable()) {
                    change = new VariableModifierChange(refactoring);
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
                if (isMethod()) {
                    change = new MethodAnnotationChange(refactoring);
                } else if (isVariable()) {
                    change = new VariableAnnotationChange(refactoring);
                }
                break;
            }
            case TYPE_CHANGE: {
                if (isVariable()) {
                    change = new VariableAnnotationChange(refactoring);
                }
                break;
            }
            default:
                throw new RuntimeException("Something is wrong!!!!!!!");
        }

        return change;
    }

    private boolean isMethod() {
        return "method".equals(elementType);
    }

    private boolean isVariable() {
        return "variable".equals(elementType);
    }
}
