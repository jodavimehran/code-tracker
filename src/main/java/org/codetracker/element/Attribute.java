package org.codetracker.element;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.StatementObject;

import org.codetracker.api.Version;

import static org.codetracker.util.Util.annotationsToString;
import static org.codetracker.util.Util.getPath;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

public class Attribute extends BaseCodeElement {
    private final UMLAttribute umlAttribute;

    private Attribute(UMLAttribute umlAttribute, String identifierExcludeVersion, String name, String filePath, Version version) {
        super(identifierExcludeVersion, name, filePath, version);
        this.umlAttribute = umlAttribute;
    }

    public static Attribute of(UMLAttribute umlAttribute, Version version) {
        String sourceFolder = getPath(umlAttribute.getLocationInfo().getFilePath(), umlAttribute.getClassName());
        String name = umlAttribute.getName();
        String className = umlAttribute.getClassName();
        String visibility = umlAttribute.getVisibility().toString();
        String type = umlAttribute.getType().toString();
        int startLine = umlAttribute.getLocationInfo().getStartLine();

        String modifiersString = new ModifiersBuilder()
                .isFinal(umlAttribute.isFinal())
                .isStatic(umlAttribute.isStatic())
                .isTransient(umlAttribute.isTransient())
                .isVolatile(umlAttribute.isVolatile())
                .build();

        String identifierExcludeVersion = String.format("%s%s@%s(%s)%s:%s%s", sourceFolder, className, modifiersString, visibility, name, type, annotationsToString(umlAttribute.getAnnotations()));
        return new Attribute(umlAttribute, identifierExcludeVersion, String.format("%s%s@%s(%s)%s:%s(%d)", sourceFolder, className, modifiersString, visibility, name, type, startLine), umlAttribute.getLocationInfo().getFilePath(), version);
    }

    public Block findBlockWithoutName(Predicate<Block> equalOperator) {
        for (UMLAnonymousClass anonymousClass : umlAttribute.getAnonymousClassList()) {
            for (UMLOperation operation : anonymousClass.getOperations()) {
                if (operation.getBody() != null) {
                	//first process leaves then composites
                	for (AbstractCodeFragment leaf : operation.getBody().getCompositeStatement().getLeaves()) {
                    	if (leaf instanceof StatementObject) {
        	                Block block = Block.of((StatementObject)leaf, this);
        	                if (block != null && equalOperator.test(block)) {
        	                    return block;
        	                }
                    	}
                    }
                	Set<Block> matches = new LinkedHashSet<Block>();
                    for (CompositeStatementObject composite : operation.getBody().getCompositeStatement().getInnerNodes()) {
                        Block block = Block.of(composite, this);
                        if (block != null && equalOperator.test(block)) {
                        	matches.add(block);
                        }
                    }
                    for (Block match : matches) {
                    	if (!match.getLocation().getCodeElementType().equals(CodeElementType.BLOCK)) {
                    		return match;
                    	}
                    }
                }
            }
        }
        for (LambdaExpressionObject lambda : umlAttribute.getAllLambdas()) {
            if (lambda.getBody() != null) {
            	//first process leaves then composites
            	for (AbstractCodeFragment leaf : lambda.getBody().getCompositeStatement().getLeaves()) {
                	if (leaf instanceof StatementObject) {
    	                Block block = Block.of((StatementObject)leaf, this);
    	                if (block != null && equalOperator.test(block)) {
    	                    return block;
    	                }
                	}
                }
            	Set<Block> matches = new LinkedHashSet<Block>();
                for (CompositeStatementObject composite : lambda.getBody().getCompositeStatement().getInnerNodes()) {
                    Block block = Block.of(composite, this);
                    if (block != null && equalOperator.test(block)) {
                    	matches.add(block);
                    }
                }
                for (Block match : matches) {
                	if (!match.getLocation().getCodeElementType().equals(CodeElementType.BLOCK)) {
                		return match;
                	}
                }
            }
        }
        return null;
    }

    public UMLAttribute getUmlAttribute() {
        return umlAttribute;
    }

    @Override
    public LocationInfo getLocation() {
        return umlAttribute.getLocationInfo();
    }
}
