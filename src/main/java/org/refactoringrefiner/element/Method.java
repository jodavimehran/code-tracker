package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.refactoringrefiner.api.Version;
import org.refactoringrefiner.util.Util;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Method extends BaseCodeElement {
    private final UMLOperation umlOperation;
    private final String identifierIgnoringVersionAndDocumentation;
    private final String identifierIgnoringVersionAndDocumentationAndBody;

    private Method(UMLOperation umlOperation, String identifierIgnoringVersion, String identifierIgnoringVersionAndDocumentation, String identifierIgnoringVersionAndDocumentationAndBody, String name, String filePath, Version version) {
        super(identifierIgnoringVersion, name, filePath, version);
        this.umlOperation = umlOperation;
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

    public UMLOperation getUmlOperation() {
        return umlOperation;
    }

    public boolean equalIdentifierIgnoringVersionAndDocument(Method method) {
        return this.identifierIgnoringVersionAndDocumentation.equals(method.identifierIgnoringVersionAndDocumentation);
    }

    public boolean equalIdentifierIgnoringVersionAndDocumentAndBody(Method method) {
        return this.identifierIgnoringVersionAndDocumentationAndBody.equals(method.identifierIgnoringVersionAndDocumentationAndBody);
    }

    public String getIdentifierIgnoringVersionAndDocumentation() {
        return identifierIgnoringVersionAndDocumentation;
    }

    public String getIdentifierIgnoringVersionAndDocumentationAndBody() {
        return identifierIgnoringVersionAndDocumentationAndBody;
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

//    /**
//     *
//     */
//    public static class MethodElementDiff extends BaseClassMemberElementDiff<Method> {
//
//        public MethodElementDiff(Method leftSide, Method rightSide) {
//            super(leftSide, rightSide);
//        }
//
//
//        protected Refactoring getRenameRefactoring() {
//            return new RenameOperationRefactoring(this.leftSide.info, this.rightSide.info);
//        }
//
//        protected Refactoring getMoveRefactoring() {
//            return getRefactoring();
//        }
//
//        protected Refactoring getMoveAndRenameRefactoring() {
//            return getRefactoring();
//        }
//
//        protected Set<Refactoring> getOtherRefactorings() {
//            return new UMLOperationDiff(this.leftSide.info, this.rightSide.info).getRefactorings();
//        }
//
//        private Refactoring getRefactoring() {
//            if (isSameType(rightSide.info.getClassName(), leftSide.info.getSuperclass()))
//                return new PullUpOperationRefactoring(leftSide.info, rightSide.info);
//
//            if (isSameType(leftSide.info.getClassName(), rightSide.info.getSuperclass()))
//                return new PushDownOperationRefactoring(leftSide.info, rightSide.info);
//
//            return new MoveOperationRefactoring(this.leftSide.info, this.rightSide.info);
//        }
//
//
//    }
//
//    public static class ExtractRefactoringBuilder {
//        private final Method extractedOperation;
//        private final Method sourceOperationBeforeExtraction;
//        private final Method sourceOperationAfterExtraction;
//
//        public ExtractRefactoringBuilder(Method extractedOperation, Method sourceOperationBeforeExtraction, Method sourceOperationAfterExtraction) {
//            this.extractedOperation = extractedOperation;
//            this.sourceOperationBeforeExtraction = sourceOperationBeforeExtraction;
//            this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
//        }
//
//        public Refactoring getRefactoring() {
//            return new ExtractOperationRefactoring(null, this.extractedOperation.info, this.sourceOperationBeforeExtraction.info, this.sourceOperationAfterExtraction.info, new ArrayList<>());
//        }
//    }
//
//    public static class InlineRefactoringBuilder {
//        private final Method inlinedOperation;
//        private final Method targetOperationBeforeInline;
//        private final Method targetOperationAfterInline;
//
//        public InlineRefactoringBuilder(Method inlinedOperation, Method targetOperationBeforeInline, Method targetOperationAfterInline) {
//            this.inlinedOperation = inlinedOperation;
//            this.targetOperationBeforeInline = targetOperationBeforeInline;
//            this.targetOperationAfterInline = targetOperationAfterInline;
//        }
//
//        public Refactoring getRefactoring() {
//            return new InlineOperationRefactoring(this.inlinedOperation.info, this.targetOperationBeforeInline.info, this.targetOperationAfterInline.info);
//        }
//    }
}
