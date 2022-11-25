package org.codetracker.element;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.codetracker.api.Version;
import org.codetracker.util.Util;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Method extends BaseCodeElement {
    private final VariableDeclarationContainer umlOperation;
    private final String documentsHashCode;
    private final String identifierIgnoringVersionAndDocumentationAndBody;

    private Method(VariableDeclarationContainer umlOperation, String identifierIgnoringVersion, String identifierIgnoringVersionAndDocumentationAndBody, String name, String filePath, Version version) {
        super(identifierIgnoringVersion, name, filePath, version);
        this.umlOperation = umlOperation;
        this.documentsHashCode = getDocumentsSha512(umlOperation);
        this.identifierIgnoringVersionAndDocumentationAndBody = identifierIgnoringVersionAndDocumentationAndBody;
    }

    public static Method of(VariableDeclarationContainer umlOperation, Version version) {
        String sourceFolder = Util.getPath(umlOperation.getLocationInfo().getFilePath(), umlOperation.getClassName());
        String identifierIgnoringVersion = null;
        String identifierIgnoringVersionAndDocumentationAndBody = null;
        String name = null;
        if (umlOperation instanceof UMLOperation) {
            identifierIgnoringVersion = getIdentifierExcludeVersion((UMLOperation) umlOperation, true, true, true);
            identifierIgnoringVersionAndDocumentationAndBody = getIdentifierExcludeVersion((UMLOperation) umlOperation, false, false, true);
            name = String.format("%s%s", sourceFolder, ((UMLOperation) umlOperation).getKey());
        }
        else if (umlOperation instanceof UMLInitializer) {
            identifierIgnoringVersion = getIdentifierExcludeVersion((UMLInitializer) umlOperation, true, true);
            identifierIgnoringVersionAndDocumentationAndBody = getIdentifierExcludeVersion((UMLInitializer) umlOperation, false, false);
            name = String.format("%s%s", sourceFolder, umlOperation.getName());
        }
        //TODO support UMLAttribute in the future
        return new Method(umlOperation, identifierIgnoringVersion, identifierIgnoringVersionAndDocumentationAndBody, name, umlOperation.getLocationInfo().getFilePath(), version);
    }

    public Variable findVariable(Predicate<Variable> equalOperator) {
        for (VariableDeclaration variableDeclaration : umlOperation.getAllVariableDeclarations()) {
            Variable variable = Variable.of(variableDeclaration, this);
            if (equalOperator.test(variable)) {
                return variable;
            }
        }
        for (UMLAnonymousClass anonymousClass : umlOperation.getAnonymousClassList()) {
            for (UMLOperation operation : anonymousClass.getOperations()) {
                for (VariableDeclaration variableDeclaration : operation.getAllVariableDeclarations()) {
                    Variable variable = Variable.of(variableDeclaration, this);
                    if (equalOperator.test(variable)) {
                        return variable;
                    }
                }
            }
        }
        for (LambdaExpressionObject lambda : umlOperation.getAllLambdas()) {
            for (VariableDeclaration parameter : lambda.getParameters()) {
                Variable variable = Variable.of(parameter, this);
                if (equalOperator.test(variable)) {
                    return variable;
                }
            }
        }
        return null;
    }

    public Block findBlock(Predicate<Block> equalOperator) {
        if (umlOperation.getBody() != null) {
            for (AbstractStatement composite : umlOperation.getBody().getCompositeStatement().getAllStatements()) {
                Block block = Block.of(composite, this);
                if (equalOperator.test(block)) {
                    return block;
                }
            }
        }
        for (UMLAnonymousClass anonymousClass : umlOperation.getAnonymousClassList()) {
            for (UMLOperation operation : anonymousClass.getOperations()) {
                if (operation.getBody() != null) {
                    for (AbstractStatement composite : operation.getBody().getCompositeStatement().getAllStatements()) {
                        Block block = Block.of(composite, this);
                        if (equalOperator.test(block)) {
                            return block;
                        }
                    }
                }
            }
        }
        for (LambdaExpressionObject lambda : umlOperation.getAllLambdas()) {
            if (lambda.getBody() != null) {
                for (AbstractStatement composite : lambda.getBody().getCompositeStatement().getAllStatements()) {
                    Block block = Block.of(composite, this);
                    if (equalOperator.test(block)) {
                        return block;
                    }
                }
            }
        }
        return null;
    }

    public VariableDeclarationContainer getUmlOperation() {
        return umlOperation;
    }

    public boolean equalIdentifierIgnoringVersionAndDocumentAndBody(Method method) {
        return this.identifierIgnoringVersionAndDocumentationAndBody.equals(method.identifierIgnoringVersionAndDocumentationAndBody);
    }

    public boolean equalDocuments(Method method) {
        if (this.documentsHashCode == null && method.documentsHashCode == null) return true;

        if (this.documentsHashCode == null || method.documentsHashCode == null) {
            return false;
        }
        return this.documentsHashCode.equals(method.documentsHashCode);
    }

    public boolean equalBody(Method method) {
        if (this.umlOperation.getBody() == null && method.umlOperation.getBody() == null) return true;

        if (this.umlOperation.getBody() == null || method.umlOperation.getBody() == null) {
            return false;
        }
        return this.umlOperation.getBody().getBodyHashCode() == method.umlOperation.getBody().getBodyHashCode();
    }

    public static String getIdentifierExcludeVersion(UMLOperation info, boolean containsBody, boolean containsDocumentation, boolean containsAnnotations) {
        StringBuilder sb = new StringBuilder();
        sb.append(Util.getPath(info.getLocationInfo().getFilePath(), info.getClassName()));
        sb.append(info.getClassName());
        sb.append(String.format("#(%s)", info.getVisibility()));

        List<String> modifiers = new ArrayList<>();
        if (info.isStatic())
            modifiers.add("static");
        if (info.isAbstract())
            modifiers.add("abstract");
        if (info.isFinal())
            modifiers.add("final");

        if (info.isSynchronized())
            modifiers.add("synchronized");

        if (!modifiers.isEmpty()) {
            modifiers.sort(String::compareTo);
            sb.append(String.format("(%s)", String.join(",", modifiers)));
        }

        sb.append(info.getName());
        sb.append("(");
        sb.append(info.getParametersWithoutReturnType().stream().map(Method.MethodParameter::new).map(Objects::toString).collect(Collectors.joining(",")));
        sb.append(")");
        if (info.getReturnParameter() != null) {
            sb.append(":");
            sb.append(info.getReturnParameter());
        }
        if (!info.getThrownExceptionTypes().isEmpty()) {
            sb.append("[");
            sb.append(info.getThrownExceptionTypes().stream().map(Object::toString).collect(Collectors.joining(",")));
            sb.append("]");
        }
        if (containsBody && info.getBody() != null) {
            sb.append("{");
            sb.append(info.getBody().getBodyHashCode());
            sb.append("}");
        }
        if (containsDocumentation && !info.getComments().isEmpty()) {
            sb.append("{");
            sb.append(getDocumentsSha512(info));
            sb.append("}");
        }
        if (containsAnnotations) {
            sb.append(Util.annotationsToString(info.getAnnotations()));
        }
        return sb.toString();
    }

    public static String getIdentifierExcludeVersion(UMLInitializer info, boolean containsBody, boolean containsDocumentation) {
        StringBuilder sb = new StringBuilder();
        sb.append(Util.getPath(info.getLocationInfo().getFilePath(), info.getClassName()));
        sb.append(info.getClassName());
        sb.append("#");

        List<String> modifiers = new ArrayList<>();
        if (info.isStatic())
            modifiers.add("static");

        if (!modifiers.isEmpty()) {
            modifiers.sort(String::compareTo);
            sb.append(String.format("(%s)", String.join(",", modifiers)));
        }

        sb.append(info.getName());

        if (containsBody && info.getBody() != null) {
            sb.append("{");
            sb.append(info.getBody().getBodyHashCode());
            sb.append("}");
        }
        if (containsDocumentation && !info.getComments().isEmpty()) {
            sb.append("{");
            sb.append(getDocumentsSha512(info));
            sb.append("}");
        }
        return sb.toString();
    }

    public static String getDocumentsSha512(VariableDeclarationContainer info) {
        if (info.getComments().isEmpty())
            return null;
        return Util.getSHA512(info.getComments().stream().map(UMLComment::getText).collect(Collectors.joining(";")));
    }

    @Override
    public LocationInfo getLocation() {
        return umlOperation.getLocationInfo();
    }

    public static class MethodParameter {
        private final UMLParameter info;
        private final Set<UMLAnnotation> annotations;

        public MethodParameter(UMLParameter info) {
            this.info = info;
            annotations = new HashSet<>(info.getAnnotations());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodParameter that = (MethodParameter) o;
            return Objects.equals(this.info, that.info) &&
                    Objects.equals(this.info.getVariableDeclaration().isFinal(), that.info.getVariableDeclaration().isFinal()) &&
                    Objects.equals(this.info.getName(), that.info.getName()) &&
                    Objects.equals(this.annotations, that.annotations);
        }

        @Override
        public int hashCode() {
            return Objects.hash(info, info.getName(), annotations, info.getVariableDeclaration().isFinal());
        }

        @Override
        public String toString() {
            return (this.info.getVariableDeclaration().isFinal() ? "(final)" : "") + info.toString().replace(" ", ":") + Util.annotationsToString(info.getAnnotations());
        }

    }
}
