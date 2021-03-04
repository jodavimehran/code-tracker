package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.diff.*;
import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Version;

import java.util.*;
import java.util.stream.Collectors;

public class Method extends BaseCodeElement<UMLOperation> {
    private final Set<UMLType> thrownExceptionTypes;
    private final Set<UMLAnnotation> annotations;
    private final List<MethodParameter> parameters;

    public Method(UMLOperation info, Version version) {
        super(info, version);
        thrownExceptionTypes = new HashSet<>(this.info.getThrownExceptionTypes());
        annotations = new HashSet<>(this.info.getAnnotations());
        parameters = getInfo().getParametersWithoutReturnType().stream().map(MethodParameter::new).collect(Collectors.toList());
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Method that = (Method) o;
//
//        return new EqualsBuilder()
//                .append(this.info.getName(), that.info.getName())
//                .append(this.info.getClassName(), that.info.getClassName())
//                .append(this.info.getLocationInfo().getFilePath(), that.info.getLocationInfo().getFilePath())
//                .append(this.getVersion().getId(), that.getVersion().getId())
//                .isEquals()
//                && this.info.equalParameterTypes(that.info)
//                && this.info.equalParameterNames(that.info)
//                && new HashSet<>(this.info.getAnnotations()).containsAll(new HashSet<>(that.info.getAnnotations()));
//    }
//
//    @Override
//    public int hashCode() {
//        return new HashCodeBuilder(37, 57)
//                .append(this.info.getName())
//                .append(this.info.getTypeParameters())
//                .append(this.info.getReturnParameter())
//                .append(this.info.getClassName())
//                .append(this.info.getLocationInfo().getFilePath())
//                .append(this.getVersion().getId())
//                .append(this.info.getAnnotations())
//                .append(this.info.getBody().getSha512())
//                .toHashCode();
//    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Method method = (Method) o;
        if ((this.info.getBody() == null && method.info.getBody() != null) || (this.info.getBody() != null && method.info.getBody() == null))
            return false;
        return Objects.equals(this.info.getName(), method.info.getName()) &&
                Objects.equals(this.info.getVisibility(), method.info.getVisibility()) &&
                Objects.equals(this.info.getClassName(), method.info.getClassName()) &&
                Objects.equals(this.info.getLocationInfo().getFilePath(), method.info.getLocationInfo().getFilePath()) &&
                Objects.equals(this.getVersion().getId(), method.getVersion().getId()) &&
                Objects.equals(this.parameters, method.parameters) &&
                this.thrownExceptionTypes.containsAll(method.thrownExceptionTypes) &&
                this.annotations.contains(method.annotations) &&
                (this.info.getBody() == null || this.info.getBody().equals(method.info.getBody()))
                ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.info.getVisibility(),
                this.info.getName(),
                this.parameters,
                this.info.getReturnParameter(),
                this.info.getClassName(),
                this.info.getLocationInfo().getFilePath(),
                this.annotations,
                this.thrownExceptionTypes,
                this.info.getBody(),
                this.getVersion().getId()
        );
    }

    @Override
    public String toString() {
        return getIdentifier();
    }

    @Override
    public String getIdentifierExcludeVersion() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFullName());
        sb.append("(");
        sb.append(this.parameters.stream().map(Objects::toString).collect(Collectors.joining(",")));
        sb.append("):");
        sb.append(this.info.getReturnParameter());
        if (!this.thrownExceptionTypes.isEmpty()) {
            sb.append("[");
            sb.append(this.thrownExceptionTypes.stream().map(Object::toString).collect(Collectors.joining(",")));
            sb.append("]");
        }
        if (this.info.getBody() != null) {
            sb.append("{");
            sb.append(this.info.getBody().getSha512());
            sb.append("}");
        }
        sb.append(annotationsToString());
        return sb.toString();
    }

    @Override
    protected List<UMLAnnotation> getAnnotations() {
        return this.info.getAnnotations();
    }

    @Override
    public String getFullName() {
        return getPath(this.info.getLocationInfo().getFilePath(), this.info.getClassName()) +
                this.info.getClassName() +
                String.format("#(%s)", this.info.getVisibility()) +
                this.info.getName();
    }

    @Override
    public String getName() {
        return this.info.getName();
    }

    @Override
    public String getContainerName() {
        return getPath(this.info.getLocationInfo().getFilePath(), this.info.getClassName()) + this.info.getClassName();
    }

    @Override
    public String getSourceFolder() {
        return getPath(this.info.getLocationInfo().getFilePath(), this.info.getName());
    }

    @Override
    public String getPackageName() {
        return getPackage(this.info.getLocationInfo().getFilePath(), this.info.getClassName());
    }

    /**
     *
     */
    public static class MethodElementDiff extends BaseClassMemberElementDiff<Method> {

        public MethodElementDiff(Method leftSide, Method rightSide) {
            super(leftSide, rightSide);
        }


        protected Refactoring getRenameRefactoring() {
            return new RenameOperationRefactoring(this.leftSide.info, this.rightSide.info);
        }

        protected Refactoring getMoveRefactoring() {
            return getRefactoring();
        }

        protected Refactoring getMoveAndRenameRefactoring() {
            return getRefactoring();
        }

        protected Set<Refactoring> getOtherRefactorings() {
            return new UMLOperationDiff(this.leftSide.info, this.rightSide.info).getRefactorings();
        }

        private Refactoring getRefactoring() {
            if (isSameType(rightSide.info.getClassName(), leftSide.info.getSuperclass()))
                return new PullUpOperationRefactoring(leftSide.info, rightSide.info);

            if (isSameType(leftSide.info.getClassName(), rightSide.info.getSuperclass()))
                return new PushDownOperationRefactoring(leftSide.info, rightSide.info);

            return new MoveOperationRefactoring(this.leftSide.info, this.rightSide.info);
        }


    }

    public static class ExtractRefactoringBuilder {
        private final Method extractedOperation;
        private final Method sourceOperationBeforeExtraction;
        private final Method sourceOperationAfterExtraction;

        public ExtractRefactoringBuilder(Method extractedOperation, Method sourceOperationBeforeExtraction, Method sourceOperationAfterExtraction) {
            this.extractedOperation = extractedOperation;
            this.sourceOperationBeforeExtraction = sourceOperationBeforeExtraction;
            this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
        }

        public Refactoring getRefactoring() {
            return new ExtractOperationRefactoring(null, this.extractedOperation.info, this.sourceOperationBeforeExtraction.info, this.sourceOperationAfterExtraction.info, new ArrayList<>());
        }
    }

    public static class InlineRefactoringBuilder {
        private final Method inlinedOperation;
        private final Method targetOperationBeforeInline;
        private final Method targetOperationAfterInline;

        public InlineRefactoringBuilder(Method inlinedOperation, Method targetOperationBeforeInline, Method targetOperationAfterInline) {
            this.inlinedOperation = inlinedOperation;
            this.targetOperationBeforeInline = targetOperationBeforeInline;
            this.targetOperationAfterInline = targetOperationAfterInline;
        }

        public Refactoring getRefactoring() {
            return new InlineOperationRefactoring(this.inlinedOperation.info, this.targetOperationBeforeInline.info, this.targetOperationAfterInline.info);
        }
    }
}
