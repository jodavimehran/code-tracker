package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.refactoringrefiner.api.Version;
import org.refactoringrefiner.util.Util;

public class Variable extends BaseCodeElement {
    private final VariableDeclaration variableDeclaration;
    private final UMLOperation operation;
    private final String identifierIgnoringVersionAndContainer;

    private Variable(VariableDeclaration variableDeclaration, UMLOperation operation, String identifierExcludeVersion, String name, String filePath, Version version) {
        super(identifierExcludeVersion, name, filePath, version);
        this.variableDeclaration = variableDeclaration;
        this.operation = operation;
        this.identifierIgnoringVersionAndContainer = identifierExcludeVersion.substring(identifierExcludeVersion.indexOf('$'));
    }

    public static Variable of(VariableDeclaration variableDeclaration, UMLOperation operation, Version version) {
        Method method = Method.of(operation, version);
        return of(variableDeclaration, method);
    }

    public static Variable of(VariableDeclaration variableDeclaration, Method method) {
        String name = String.format("%s$%s", method.getName(), variableDeclaration.getVariableName());
        String identifierExcludeVersion = String.format(
                "%s$%s%s:%s%s",
                method.getIdentifierIgnoringVersionAndDocumentation(),
                variableDeclaration.isFinal() ? "(final)" : "",
                variableDeclaration.getVariableName(),
                variableDeclaration.getType().toQualifiedString(),
                Util.annotationsToString(variableDeclaration.getAnnotations())
        );
        return new Variable(variableDeclaration, method.getUmlOperation(), identifierExcludeVersion, name, variableDeclaration.getLocationInfo().getFilePath(), method.getVersion());
    }

    public VariableDeclaration getVariableDeclaration() {
        return variableDeclaration;
    }

    public UMLOperation getOperation() {
        return operation;
    }

    public boolean equalIdentifierIgnoringVersionAndContainer(Variable variable) {
        return this.identifierIgnoringVersionAndContainer.equals(variable.identifierIgnoringVersionAndContainer);
    }

    //    public static class VariableElementDiff extends BaseCodeElement.BaseElementDiff<Variable> {
//
//        public VariableElementDiff(Variable leftSide, Variable rightSide) {
//            super(leftSide, rightSide);
//        }
//
//        public Set<Refactoring> getRefactorings() {
//            Set<Refactoring> results = new HashSet<>();
//
//            boolean isTypeChanged = !this.leftSide.info.getType().equals(this.rightSide.info.getType());
//            if (isTypeChanged) {
//                results.add(new ChangeVariableTypeRefactoring(this.leftSide.info, this.rightSide.info, this.leftSide.method.getInfo(), this.rightSide.method.getInfo(), Collections.EMPTY_SET));
//            }
//
//            boolean isRenamed = !this.leftSide.getName().equals(this.rightSide.getName());
//            if (isRenamed) {
//                results.add(new RenameVariableRefactoring(this.leftSide.info, this.rightSide.info, this.leftSide.method.getInfo(), this.rightSide.method.getInfo(), Collections.EMPTY_SET));
//            }
//
//            if (!(isRenamed || isTypeChanged)) {
////                results.add(new ChangeVariableScopeRefactoring(this.leftSide.info, this.rightSide.info, this.leftSide.method.getInfo(), this.rightSide.method.getInfo()));
//            }
//            return results;
//        }
//
//    }
}
