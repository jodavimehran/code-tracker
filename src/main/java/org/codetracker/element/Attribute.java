package org.codetracker.element;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLJavadoc;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.StatementObject;

import org.codetracker.api.Version;
import org.refactoringminer.util.PrefixSuffixUtils;

import static org.codetracker.util.Util.annotationsToString;
import static org.codetracker.util.Util.getPath;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class Attribute extends BaseCodeElement {
    private final UMLAttribute umlAttribute;

    private Attribute(UMLAttribute umlAttribute, String identifierExcludeVersion, String name, String filePath, Version version) {
        super(identifierExcludeVersion, name, filePath, version);
        this.umlAttribute = umlAttribute;
    }

    public BaseCodeElement of(Version version) {
    	return of(this.umlAttribute, version);
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

    public boolean differInFormatting(Attribute other) {
    	String thisSignature = umlAttribute.getVariableDeclaration().getActualSignature();
		String otherSignature = other.umlAttribute.getVariableDeclaration().getActualSignature();
		if (thisSignature != null && otherSignature != null) {
    		if(!thisSignature.equals(otherSignature)) {
    			//whitespace change
    			if(thisSignature.replaceAll("\\s+","").equals(otherSignature.replaceAll("\\s+",""))) {
    				return true;
    			}
    			else {
    				String prefix = PrefixSuffixUtils.longestCommonPrefix(thisSignature, otherSignature);
    				String suffix = PrefixSuffixUtils.longestCommonSuffix(thisSignature, otherSignature);
    				if(!prefix.isEmpty() && !suffix.isEmpty()) {
    					return true;
    				}
    			}
    		}
    	}
    	return false;
    }

	public int signatureStartLine() {
		int signatureStartLine = -1;
		if (umlAttribute instanceof UMLEnumConstant) {
			int endLine = -1;
			if (umlAttribute.getJavadoc() != null) {
				endLine = umlAttribute.getJavadoc().getLocationInfo().getEndLine();
			}
			if (umlAttribute.getAnnotations().size() > 0) {
				UMLAnnotation lastAnnotation = umlAttribute.getAnnotations().get(umlAttribute.getAnnotations().size()-1);
				if (lastAnnotation.getLocationInfo().getEndLine() > endLine) {
					endLine = lastAnnotation.getLocationInfo().getEndLine();
				}
			}
			if (endLine == -1) {
				signatureStartLine = umlAttribute.getLocationInfo().getStartLine();
			}
			else {
				signatureStartLine = endLine + 1;
			}
		}
		else {
			signatureStartLine = umlAttribute.getType().getLocationInfo().getStartLine();
		}
		return signatureStartLine;		
	}

    public boolean isMultiLine() {
    	if (umlAttribute.getVariableDeclaration().getActualSignature() != null) {
    		return umlAttribute.getVariableDeclaration().getActualSignature().contains("\n");
    	}
    	return false;
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
                	Map<CodeElementType, Block> matches = new LinkedHashMap<CodeElementType, Block>();
                    for (CompositeStatementObject composite : operation.getBody().getCompositeStatement().getInnerNodes()) {
                        Block block = Block.of(composite, this);
                        if (block != null && equalOperator.test(block)) {
                        	matches.put(block.getLocation().getCodeElementType(), block);
                        }
                    }
                    Block block = promotionStrategy(matches);
                    if (block != null) {
                    	return block;
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
            	Map<CodeElementType, Block> matches = new LinkedHashMap<CodeElementType, Block>();
                for (CompositeStatementObject composite : lambda.getBody().getCompositeStatement().getInnerNodes()) {
                    Block block = Block.of(composite, this);
                    if (block != null && equalOperator.test(block)) {
                    	matches.put(block.getLocation().getCodeElementType(), block);
                    }
                }
                Block block = promotionStrategy(matches);
                if (block != null) {
                	return block;
                }
            }
        }
        return null;
    }

    public Comment findComment(Predicate<Comment> equalOperator) {
        for (UMLComment umlComment : umlAttribute.getComments()) {
            Comment comment = Comment.of(umlComment, this);
            if (comment != null && equalOperator.test(comment)) {
                return comment;
            }
        }
        UMLJavadoc javadoc = umlAttribute.getJavadoc();
    	if (javadoc != null) {
    		Comment comment = Comment.of(javadoc, this);
    		if (comment != null && equalOperator.test(comment)) {
                return comment;
            }
    	}
        for (UMLAnonymousClass anonymousClass : umlAttribute.getAnonymousClassList()) {
            for (UMLOperation operation : anonymousClass.getOperations()) {
                for (UMLComment umlComment : operation.getComments()) {
                    Comment comment = Comment.of(umlComment, this);
                    if (comment != null && equalOperator.test(comment)) {
                        return comment;
                    }
                }
            }
        }
        for (LambdaExpressionObject lambda : umlAttribute.getAllLambdas()) {
            for (UMLComment umlComment : lambda.getComments()) {
                Comment comment = Comment.of(umlComment, this);
                if (comment != null && equalOperator.test(comment)) {
                    return comment;
                }
            }
        }
        return null;
    }

    public Annotation findAnnotation(Predicate<Annotation> equalOperator) {
        for (UMLAnnotation umlAnnotation : umlAttribute.getAnnotations()) {
        	Annotation annotation = Annotation.of(umlAnnotation, this);
            if (annotation != null && equalOperator.test(annotation)) {
                return annotation;
            }
        }
        for (UMLAnonymousClass anonymousClass : umlAttribute.getAnonymousClassList()) {
            for (UMLOperation operation : anonymousClass.getOperations()) {
                for (UMLAnnotation umlAnnotation : operation.getAnnotations()) {
                	Annotation annotation = Annotation.of(umlAnnotation, this);
                    if (annotation != null && equalOperator.test(annotation)) {
                        return annotation;
                    }
                }
            }
        }
        return null;
    }

	private Block promotionStrategy(Map<CodeElementType, Block> matches) {
		//promote catch over try
		if (matches.containsKey(CodeElementType.CATCH_CLAUSE)) {
			return matches.get(CodeElementType.CATCH_CLAUSE);
		}
		if (matches.containsKey(CodeElementType.FINALLY_BLOCK)) {
			return matches.get(CodeElementType.FINALLY_BLOCK);
		}
		for (CodeElementType key : matches.keySet()) {
			if (!key.equals(CodeElementType.BLOCK)) {
				return matches.get(key);
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
