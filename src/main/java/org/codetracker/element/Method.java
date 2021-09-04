package org.codetracker.element;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.codetracker.api.Version;
import org.codetracker.util.Util;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class Method extends BaseCodeElement {
    private final UMLOperation umlOperation;
    private final String documentsHashCode;
    private final String identifierIgnoringVersionAndDocumentationAndBody;
    private final String identifierIgnoringVersionAndDocumentation;

    private Method(UMLOperation umlOperation, String identifierIgnoringVersion, String identifierIgnoringVersionAndDocumentation, String identifierIgnoringVersionAndDocumentationAndBody, String name, String filePath, Version version) {
        super(identifierIgnoringVersion, name, filePath, version);
        this.umlOperation = umlOperation;
        this.documentsHashCode = Util.getDocumentsSha512(umlOperation);
        this.identifierIgnoringVersionAndDocumentation = identifierIgnoringVersionAndDocumentation;
        this.identifierIgnoringVersionAndDocumentationAndBody = identifierIgnoringVersionAndDocumentationAndBody;
    }

    public static Method of(UMLOperation umlOperation, Version version) {
        String sourceFolder = Util.getPath(umlOperation.getLocationInfo().getFilePath(), umlOperation.getClassName());
        String identifierIgnoringVersion = Util.getIdentifierExcludeVersion(umlOperation, true, true);
        String identifierIgnoringVersionAndDocumentation = Util.getIdentifierExcludeVersion(umlOperation, true, false);
        String identifierIgnoringVersionAndDocumentationAndBody = Util.getIdentifierExcludeVersion(umlOperation, false, false);
        String name = String.format("%s%s", sourceFolder, umlOperation.getKey());
        return new Method(umlOperation, identifierIgnoringVersion, identifierIgnoringVersionAndDocumentation, identifierIgnoringVersionAndDocumentationAndBody, name, umlOperation.getLocationInfo().getFilePath(), version);
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
        return null;
    }

    public UMLOperation getUmlOperation() {
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

    public boolean equalSignature(Method method) {
        return this.umlOperation.equalSignature(method.umlOperation);
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
