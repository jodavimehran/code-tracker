package org.codetracker.element;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import org.codetracker.api.Version;
import org.codetracker.util.Util;

import java.util.stream.Collectors;

public class Block extends BaseCodeElement {
    private final CompositeStatementObject composite;
    private final VariableDeclarationContainer operation;

    private Block(CompositeStatementObject composite, VariableDeclarationContainer operation, String identifierIgnoringVersion, String name, String filePath, Version version) {
        super(identifierIgnoringVersion, name, filePath, version);
        this.composite = composite;
        this.operation = operation;
    }

    public static Block of(CompositeStatementObject composite, VariableDeclarationContainer operation, Version version) {
        Method method = Method.of(operation, version);
        return of(composite, method);
    }

    public static Block of(CompositeStatementObject composite, Method method) {
        LocationInfo compositeLocationInfo = composite.getLocationInfo();
        String statementType = compositeLocationInfo.getCodeElementType().getName() != null ? compositeLocationInfo.getCodeElementType().getName() : composite.toString();
        String name = String.format("%s$%s(%d-%d)", method.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
        String sha512 = Util.getSHA512(composite.getStatements().stream().map(AbstractCodeFragment::toString).collect(Collectors.joining()));
        String identifierExcludeVersion = String.format(
                "%s$%s:{%s,%s}",
                method.getIdentifierIgnoringVersion(),
                statementType,
                sha512,
                composite.getSignature()
        );
        return new Block(composite, method.getUmlOperation(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), method.getVersion());
    }

    @Override
    public LocationInfo getLocation() {
        return composite.getLocationInfo();
    }
}
