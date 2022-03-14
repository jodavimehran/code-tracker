package org.codetracker.element;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.codetracker.api.Version;
import org.codetracker.util.Util;

import java.util.stream.Collectors;

public class Variable extends BaseCodeElement {
    private final VariableDeclaration variableDeclaration;
    private final UMLOperation operation;

    private Variable(VariableDeclaration variableDeclaration, UMLOperation operation, String identifierExcludeVersion, String name, String filePath, Version version) {
        super(identifierExcludeVersion, name, filePath, version);
        this.variableDeclaration = variableDeclaration;
        this.operation = operation;
    }

    public static Variable of(VariableDeclaration variableDeclaration, VariableDeclarationContainer operation, Version version) {
        Method method = Method.of(operation, version);
        return of(variableDeclaration, method);
    }

    public static Variable of(VariableDeclaration variableDeclaration, Method method) {
        String name = String.format("%s$%s(%d)", method.getName(), variableDeclaration.toString().replace(" ", ""), variableDeclaration.getLocationInfo().getStartLine());
        String sha512 = Util.getSHA512(variableDeclaration.getScope().getStatementsInScope().stream().map(AbstractCodeFragment::toString).collect(Collectors.joining()));
        String identifierExcludeVersion = String.format(
                "%s$%s%s:%s%s{%s,%s}",
                method.getIdentifierIgnoringVersion(),
                variableDeclaration.isFinal() ? "(final)" : "",
                variableDeclaration.getVariableName(),
                variableDeclaration.getType().toQualifiedString(),
                Util.annotationsToString(variableDeclaration.getAnnotations()),
                sha512,
                variableDeclaration.getScope().getParentSignature()
        );
        return new Variable(variableDeclaration, method.getUmlOperation(), identifierExcludeVersion, name, variableDeclaration.getLocationInfo().getFilePath(), method.getVersion());
    }

    public VariableDeclaration getVariableDeclaration() {
        return variableDeclaration;
    }

    public UMLOperation getOperation() {
        return operation;
    }

    @Override
    public LocationInfo getLocation() {
        return variableDeclaration.getLocationInfo();
    }
}
