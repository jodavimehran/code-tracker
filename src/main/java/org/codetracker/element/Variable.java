package org.codetracker.element;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.codetracker.api.Version;
import org.codetracker.util.Util;

import java.util.stream.Collectors;

public class Variable extends BaseCodeElement {
    private final VariableDeclaration variableDeclaration;
    private final VariableDeclarationContainer operation;

    private Variable(VariableDeclaration variableDeclaration, VariableDeclarationContainer operation, String identifierExcludeVersion, String name, String filePath, Version version) {
        super(identifierExcludeVersion, name, filePath, version);
        this.variableDeclaration = variableDeclaration;
        this.operation = operation;
    }

    public BaseCodeElement of(Version version) {
    	return of(this.variableDeclaration, this.operation, version);
    }

    public static Variable of(VariableDeclaration variableDeclaration, VariableDeclarationContainer operation, Version version) {
        Method method = Method.of(operation, version);
        return of(variableDeclaration, method);
    }

    public static Variable of(VariableDeclaration variableDeclaration, Method method) {
    	//check if this is a lambda parameter
    	AbstractCodeFragment lambdaStatement = null;
    	for(LambdaExpressionObject lambda : method.getUmlOperation().getAllLambdas()) {
    		if(lambda.getParameters().contains(variableDeclaration) && method.getUmlOperation().getBody() != null) {
    			for(AbstractCodeFragment leaf : method.getUmlOperation().getBody().getCompositeStatement().getLeaves()) {
    				if(leaf.getLambdas().contains(lambda)) {
    					lambdaStatement = leaf;
    					break;
    				}
    			}
    			if(lambdaStatement != null) {
    				break;
    			}
    		}
    	}
        String name = String.format("%s$%s(%d)", method.getName(), variableDeclaration.toString().replace(" ", ""), variableDeclaration.getLocationInfo().getStartLine());
        String sha512 = Util.getSHA512(variableDeclaration.getScope().getStatementsInScopeUsingVariable().stream().map(AbstractCodeFragment::toString).collect(Collectors.joining()));
        String identifierExcludeVersion = null;
        if(lambdaStatement != null) {
        	identifierExcludeVersion = String.format(
                "%s$%s%s:%s%s{%s,%s,%s}",
                method.getIdentifierIgnoringVersion(),
                variableDeclaration.isFinal() ? "(final)" : "",
                variableDeclaration.getVariableName(),
                //variableDeclaration.getType().toQualifiedString(),
                variableDeclaration.getType() != null ? variableDeclaration.getType().toQualifiedString() : null,
                Util.annotationsToString(variableDeclaration.getAnnotations()),
                sha512,
                variableDeclaration.getScope().getParentSignature(),
                Util.getSHA512(lambdaStatement.getString())
            );
        }
        else {
        	identifierExcludeVersion = String.format(
                "%s$%s%s:%s%s{%s,%s}",
                method.getIdentifierIgnoringVersion(),
                variableDeclaration.isFinal() ? "(final)" : "",
                variableDeclaration.getVariableName(),
                //variableDeclaration.getType().toQualifiedString(),
                variableDeclaration.getType() != null ? variableDeclaration.getType().toQualifiedString() : null,
                Util.annotationsToString(variableDeclaration.getAnnotations()),
                sha512,
                variableDeclaration.getScope().getParentSignature()
        	);
        }
        return new Variable(variableDeclaration, method.getUmlOperation(), identifierExcludeVersion, name, variableDeclaration.getLocationInfo().getFilePath(), method.getVersion());
    }

    public VariableDeclaration getVariableDeclaration() {
        return variableDeclaration;
    }

    public VariableDeclarationContainer getOperation() {
        return operation;
    }

    @Override
    public LocationInfo getLocation() {
        return variableDeclaration.getLocationInfo();
    }
}
