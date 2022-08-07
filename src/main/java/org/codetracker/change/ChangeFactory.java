package org.codetracker.change;

import org.codetracker.api.CodeElement;
import org.codetracker.api.Edge;
import org.codetracker.api.Version;
import org.codetracker.change.attribute.*;
import org.codetracker.change.clazz.*;
import org.codetracker.change.method.*;
import org.codetracker.change.variable.*;
import org.refactoringminer.api.Refactoring;

public final class ChangeFactory {
    private final Change.Type type;
    private final String elementType;
    private Refactoring refactoring;
    private CodeElement codeElement;
    private String comment;
    private Version parentVersion;
    private Version childVersion;

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

    public static ChangeFactory forAttribute(Change.Type type) {
        return new ChangeFactory(type, "attribute");
    }

    public static ChangeFactory forClass(Change.Type type) {
        return new ChangeFactory(type, "class");
    }

    public static ChangeFactory of(Change.Type type) {
        return new ChangeFactory(type, null);
    }

    public ChangeFactory refactoring(Refactoring refactoring) {
        this.refactoring = refactoring;
        return this;
    }

    public ChangeFactory comment(String comment) {
        this.comment = comment;
        return this;
    }

    public ChangeFactory codeElement(CodeElement codeElement) {
        this.codeElement = codeElement;
        return this;
    }

    public ChangeFactory parentVersion(Version version) {
        this.parentVersion = version;
        return this;
    }

    public ChangeFactory childVersion(Version version) {
        this.childVersion = version;
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
            case NO_CHANGE: {
                change = new NoChange();
                break;
            }
            case CONTAINER_CHANGE: {
                if (isMethod()) {
                    change = new MethodContainerChange(refactoring);
                } else if (isVariable()) {
                    change = new VariableContainerChange(refactoring);
                } else if (isAttribute()) {
                    change = new AttributeContainerChange(refactoring);
                } else if (isClass()) {
                    change = new ClassContainerChange(refactoring);
                }
                break;
            }
            case INTRODUCED: {
                if (codeElement == null)
                    throw new NullPointerException();
                if (refactoring != null && isMethod())
                    change = new Extracted(refactoring, codeElement, parentVersion, childVersion);
                else
                    change = new Introduced(codeElement, comment, refactoring);
                break;
            }
            case REMOVED: {
                change = new Removed(codeElement);
                break;
            }
            case DOCUMENTATION_CHANGE: {
                change = new DocumentationChange();
                break;
            }
            case BODY_CHANGE: {
                change = new BodyChange();
                break;
            }
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
                } else if (isAttribute()) {
                    change = new AttributeRename(refactoring);
                } else if (isClass()) {
                    change = new ClassRename(refactoring);
                }
                break;
            }
            case MOVED: {
                if (refactoring == null)
                    throw new NullPointerException();
                if (isMethod()) {
                    change = new MethodMove(refactoring);
                } else if (isAttribute()) {
                    change = new AttributeMove(refactoring);
                } else if (isClass()) {
                    change = new ClassMove(refactoring);
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
                } else if (isAttribute()) {
                    change = new AttributeModifierChange(refactoring);
                } else if (isClass()) {
                    change = new ClassModifierChange(refactoring);
                }
                break;
            }
            case ACCESS_MODIFIER_CHANGE: {
                if (refactoring == null)
                    throw new NullPointerException();
                if (isMethod()) {
                    change = new MethodAccessModifierChange(refactoring);
                } else if (isAttribute()) {
                    change = new AttributeAccessModifierChange(refactoring);
                } else if (isClass()) {
                    change = new ClassAccessModifierChange(refactoring);
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
                } else if (isAttribute()) {
                    change = new AttributeAnnotationChange(refactoring);
                } else if (isClass()) {
                    change = new ClassAnnotationChange(refactoring);
                }
                break;
            }
            case TYPE_CHANGE: {
                if (refactoring == null)
                    throw new NullPointerException();
                if (isVariable()) {
                    change = new VariableTypeChange(refactoring);
                } else if (isAttribute()) {
                    change = new AttributeTypeChange(refactoring);
                } else if (isClass()) {
                    change = new ClassDeclarationKindChange(refactoring);
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

    private boolean isAttribute() {
        return "attribute".equals(elementType);
    }

    private boolean isClass() {
        return "class".equals(elementType);
    }
}
