package org.codetracker.element;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.*;
import org.codetracker.api.Version;
import org.codetracker.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Block extends BaseCodeElement {
    private final AbstractStatement composite;
    private final VariableDeclarationContainer operation;
    private boolean closingCurlyBracket;

    private Block(AbstractStatement composite, VariableDeclarationContainer operation, String identifierIgnoringVersion, String name, String filePath, Version version) {
        super(identifierIgnoringVersion, name, filePath, version);
        this.composite = composite;
        this.operation = operation;
    }

    public boolean isClosingCurlyBracket() {
    	return closingCurlyBracket;
    }

    public void setClosingCurlyBracket(boolean closingCurlyBracket) {
    	this.closingCurlyBracket = closingCurlyBracket;
    }

    public AbstractStatement getComposite() {
        return composite;
    }

    public VariableDeclarationContainer getOperation() {
        return operation;
    }

    public static Block of(CompositeStatementObject composite, VariableDeclarationContainer operation, Version version) {
        Method method = Method.of(operation, version);
        return of(composite, method);
    }

    public static Block of(CompositeStatementObject composite, Method method) {
        LocationInfo compositeLocationInfo = composite.getLocationInfo();
        String statementType = compositeLocationInfo.getCodeElementType().getName() != null ? compositeLocationInfo.getCodeElementType().getName() : composite.toString();
        String name = String.format("%s$%s(%d-%d)", method.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
        String sha512 = Util.getSHA512(composite.getAllStatements().stream().map(AbstractCodeFragment::toString).collect(Collectors.joining()));
        String identifierExcludeVersion = String.format(
                "%s$%s:{%s,%s}",
                method.getIdentifierIgnoringVersion(),
                statementType,
                sha512,
                composite.getSignature()
        );
        return new Block(composite, method.getUmlOperation(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), method.getVersion());
    }

    public static Block of(CompositeStatementObject composite, Attribute attribute) {
        LocationInfo compositeLocationInfo = composite.getLocationInfo();
        String statementType = compositeLocationInfo.getCodeElementType().getName() != null ? compositeLocationInfo.getCodeElementType().getName() : composite.toString();
        String name = String.format("%s$%s(%d-%d)", attribute.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
        String sha512 = Util.getSHA512(composite.getAllStatements().stream().map(AbstractCodeFragment::toString).collect(Collectors.joining()));
        String identifierExcludeVersion = String.format(
                "%s$%s:{%s,%s}",
                attribute.getIdentifierIgnoringVersion(),
                statementType,
                sha512,
                composite.getSignature()
        );
        return new Block(composite, attribute.getUmlAttribute(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), attribute.getVersion());
    }

    public static Block of(StatementObject statement, VariableDeclarationContainer operation, Version version) {
        Method method = Method.of(operation, version);
        return of(statement, method);
    }

    public static Block of(StatementObject statement, Method method) {
        LocationInfo compositeLocationInfo = statement.getLocationInfo();
        List<AbstractCall> streamAPICalls = streamAPICalls(statement);
        if(streamAPICalls.size() > 0) {
            String statementType = streamAPICalls.get(0).getName();
            String name = String.format("%s$%s(%d-%d)", method.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
            String sha512 = Util.getSHA512(statement.toString());
            String identifierExcludeVersion = String.format(
                    "%s$%s:{%s,%s}",
                    method.getIdentifierIgnoringVersion(),
                    statementType,
                    sha512,
                    signature(statement, statementType)
            );
            return new Block(statement, method.getUmlOperation(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), method.getVersion());
        }
    	String statementType = statement.getLocationInfo().getCodeElementType().name();
        String name = String.format("%s$%s(%d-%d)", method.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
        String sha512 = Util.getSHA512(statement.toString());
        String identifierExcludeVersion = String.format(
                "%s$%s:{%s,%s}",
                method.getIdentifierIgnoringVersion(),
                statementType,
                sha512,
                signature(statement, statementType)
        );
        return new Block(statement, method.getUmlOperation(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), method.getVersion());
    }

    public static Block of(StatementObject statement, Attribute attribute) {
        LocationInfo compositeLocationInfo = statement.getLocationInfo();
        List<AbstractCall> streamAPICalls = streamAPICalls(statement);
        if(streamAPICalls.size() > 0) {
            String statementType = streamAPICalls.get(0).getName();
            String name = String.format("%s$%s(%d-%d)", attribute.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
            String sha512 = Util.getSHA512(statement.toString());
            String identifierExcludeVersion = String.format(
                    "%s$%s:{%s,%s}",
                    attribute.getIdentifierIgnoringVersion(),
                    statementType,
                    sha512,
                    signature(statement, statementType)
            );
            return new Block(statement, attribute.getUmlAttribute(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), attribute.getVersion());
        }
    	String statementType = statement.getLocationInfo().getCodeElementType().name();
        String name = String.format("%s$%s(%d-%d)", attribute.getName(), statementType, compositeLocationInfo.getStartLine(), compositeLocationInfo.getEndLine());
        String sha512 = Util.getSHA512(statement.toString());
        String identifierExcludeVersion = String.format(
                "%s$%s:{%s,%s}",
                attribute.getIdentifierIgnoringVersion(),
                statementType,
                sha512,
                signature(statement, statementType)
        );
        return new Block(statement, attribute.getUmlAttribute(), identifierExcludeVersion, name, compositeLocationInfo.getFilePath(), attribute.getVersion());
    }

    private static String signature(StatementObject statement, String statementType) {
        CompositeStatementObject parent = statement.getParent();
        if (parent == null) {
            return statementType;
        }
        List<AbstractStatement> sameTypeSibling = parent.getStatements().stream().filter(st -> statementType.equals(st.getLocationInfo().getCodeElementType().getName())).collect(Collectors.toList());
        int typeIndex = 1;
        for (AbstractStatement abstractStatement : sameTypeSibling) {
            if (abstractStatement.getIndex() == statement.getIndex()) {
                break;
            }
            typeIndex++;
        }
        return String.format("%s:%s%d", parent.getSignature(), statementType, typeIndex);
    }

    private static List<AbstractCall> streamAPICalls(AbstractCodeFragment statement) {
        List<AbstractCall> streamAPICalls = new ArrayList<>();
        AbstractCall invocation = statement.invocationCoveringEntireFragment();
        if(invocation == null) {
            invocation = statement.assignmentInvocationCoveringEntireStatement();
        }
        if(invocation != null && (invocation.actualString().contains(" -> ") ||
                invocation.actualString().contains("::"))) {
            for(AbstractCall inv : statement.getMethodInvocations()) {
                if(streamAPIName(inv.getName())) {
                    streamAPICalls.add(inv);
                }
            }
        }
        return streamAPICalls;
    }

    private static boolean streamAPIName(String name) {
        return name.equals("stream") || name.equals("filter") || name.equals("forEach") || name.equals("collect") || name.equals("map") || name.equals("removeIf");
    }

    public static Block of(AbstractStatement statement, VariableDeclarationContainer operation, Version version) {
        Method method = Method.of(operation, version);
        if (statement instanceof StatementObject)
            return of((StatementObject) statement, method);
        else
            return of((CompositeStatementObject) statement, method);
    }

    public static Block of(AbstractStatement statement, Method method) {
        if (statement instanceof StatementObject)
            return of((StatementObject) statement, method);
        else
            return of((CompositeStatementObject) statement, method);
    }

    @Override
    public LocationInfo getLocation() {
        return composite.getLocationInfo();
    }
}
