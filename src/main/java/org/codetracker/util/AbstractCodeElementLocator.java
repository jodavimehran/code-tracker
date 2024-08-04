package org.codetracker.util;

import java.util.List;
import java.util.function.Predicate;

import org.codetracker.api.CodeElement;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.Version;
import org.codetracker.element.Annotation;
import org.codetracker.element.Attribute;
import org.codetracker.element.Block;
import org.codetracker.element.Class;
import org.codetracker.element.Comment;
import org.codetracker.element.Import;
import org.codetracker.element.Method;
import org.codetracker.element.Package;
import org.codetracker.element.Variable;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLImport;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLPackage;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.TryStatementObject;

public abstract class AbstractCodeElementLocator {
	protected final String commitId;
	protected final String filePath;
	protected final String name;
	protected final int lineNumber;

	public AbstractCodeElementLocator(String commitId, String filePath, String name, int lineNumber) {
		this.commitId = commitId;
		this.filePath = filePath;
		this.name = name;
		this.lineNumber = lineNumber;
	}

	public AbstractCodeElementLocator(String commitId, String filePath, int lineNumber) {
		this(commitId, filePath, null, lineNumber);
	}

	public abstract CodeElement locate() throws Exception;

	protected static Method getMethod(UMLModel umlModel, Version version, String filePath, Predicate<Method> predicate) {
	    if (umlModel != null)
	        for (UMLClass umlClass : umlModel.getClassList()) {
	        	if (umlClass.getSourceFile().equals(filePath)) {
		            for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
		                Method method = getMethod(version, predicate, anonymousClass.getOperations());
		                if (method != null) return method;
		            }
		            Method method = getMethod(version, predicate, umlClass.getOperations());
		            if (method != null) return method;
	        	}
	        }
	    return null;
	}

	protected boolean classPredicateWithName(Class clazz) {
	    return clazz.getUmlClass().getNonQualifiedName().equals(name);
	}

	protected boolean classPredicateWithoutName(Class clazz) {
		for (UMLComment comment : clazz.getUmlClass().getComments()) {
			if (comment.getLocationInfo().getStartLine() <= lineNumber &&
					comment.getLocationInfo().getEndLine() >= lineNumber) {
				return true;
			}
		}
		if (clazz.getUmlClass() instanceof UMLClass) {
			UMLClass umlClass = (UMLClass) clazz.getUmlClass();
			if (umlClass.getJavadoc() != null) {
				if (umlClass.getJavadoc().getLocationInfo().getStartLine() <= lineNumber &&
						umlClass.getJavadoc().getLocationInfo().getEndLine() >= lineNumber) {
					return true;
				}
			}
			if (umlClass.getPackageDeclarationJavadoc() != null) {
				if (umlClass.getPackageDeclarationJavadoc().getLocationInfo().getStartLine() <= lineNumber &&
						umlClass.getPackageDeclarationJavadoc().getLocationInfo().getEndLine() >= lineNumber) {
					return true;
				}
			}
			for (UMLComment comment : umlClass.getPackageDeclarationComments()) {
				if (comment.getLocationInfo().getStartLine() <= lineNumber &&
						comment.getLocationInfo().getEndLine() >= lineNumber) {
					return true;
				}
			}
		}
		return clazz.getUmlClass().getLocationInfo().getStartLine() <= lineNumber &&
	            clazz.getUmlClass().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean methodPredicateWithName(Method method) {
	    return method.getUmlOperation().getName().equals(name) &&
	            method.getUmlOperation().getLocationInfo().getStartLine() <= lineNumber &&
	            method.getUmlOperation().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean methodPredicateWithoutName(Method method) {
		for (UMLComment comment : method.getUmlOperation().getComments()) {
			if (comment.getLocationInfo().getStartLine() <= lineNumber &&
					comment.getLocationInfo().getEndLine() >= lineNumber) {
				return true;
			}
		}
		if (method.getUmlOperation() instanceof UMLOperation) {
			UMLOperation umlOperation = (UMLOperation) method.getUmlOperation();
			if (umlOperation.getJavadoc() != null) {
				if (umlOperation.getJavadoc().getLocationInfo().getStartLine() <= lineNumber &&
						umlOperation.getJavadoc().getLocationInfo().getEndLine() >= lineNumber) {
					return true;
				}
			}
		}
	    return method.getUmlOperation().getLocationInfo().getStartLine() <= lineNumber &&
	            method.getUmlOperation().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean variablePredicate(Variable variable) {
	    return variable.getVariableDeclaration().getVariableName().equals(name) &&
	            variable.getVariableDeclaration().getLocationInfo().getStartLine() <= lineNumber &&
	            variable.getVariableDeclaration().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean attributePredicateWithName(Attribute attribute) {
	    return attribute.getUmlAttribute().getName().equals(name) &&
	            attribute.getUmlAttribute().getLocationInfo().getStartLine() <= lineNumber &&
	            attribute.getUmlAttribute().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean attributePredicateWithoutName(Attribute attribute) {
		for (UMLComment comment : attribute.getUmlAttribute().getComments()) {
			if (comment.getLocationInfo().getStartLine() <= lineNumber &&
					comment.getLocationInfo().getEndLine() >= lineNumber) {
				return true;
			}
		}
		if (attribute.getUmlAttribute().getJavadoc() != null) {
			if (attribute.getUmlAttribute().getJavadoc().getLocationInfo().getStartLine() <= lineNumber &&
					attribute.getUmlAttribute().getJavadoc().getLocationInfo().getEndLine() >= lineNumber) {
				return true;
			}
		}
	    return attribute.getUmlAttribute().getLocationInfo().getStartLine() <= lineNumber &&
	            attribute.getUmlAttribute().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean commentPredicate(Comment comment) {
	    return comment.getComment().getLocationInfo().getStartLine() <= lineNumber &&
	    		comment.getComment().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean annotationPredicate(Annotation annotation) {
	    return annotation.getAnnotation().getLocationInfo().getStartLine() <= lineNumber &&
	    		annotation.getAnnotation().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean packagePredicate(Package pack) {
	    return pack.getUmlPackage().getLocationInfo().getStartLine() <= lineNumber &&
	    		pack.getUmlPackage().getLocationInfo().getEndLine() >= lineNumber;
	}


	protected boolean importPredicate(Import imp) {
	    return imp.getUmlImport().getLocationInfo().getStartLine() <= lineNumber &&
	    		imp.getUmlImport().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean blockPredicate(Block block) {
		if (block.getComposite() instanceof StatementObject && block.getComposite().getAnonymousClassDeclarations().size() == 0) {
			return block.getComposite().getLocationInfo().getStartLine() <= lineNumber &&
		            block.getComposite().getLocationInfo().getEndLine() >= lineNumber;
		}
	    return block.getComposite().getLocationInfo().getStartLine() == lineNumber &&
	            block.getComposite().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean endLineBlockPredicate(Block block) {
		if (block.getComposite() instanceof TryStatementObject) {
			TryStatementObject tryStatement = (TryStatementObject)block.getComposite();
			if (tryStatement.getCatchClauses().size() > 0) {
				CompositeStatementObject catchClause = tryStatement.getCatchClauses().get(0);
				if (catchClause.getLocationInfo().getStartLine() == lineNumber || catchClause.getLocationInfo().getStartLine() == lineNumber + 1) {
					return block.getComposite().getLocationInfo().getStartLine() <= lineNumber;
				}
			}
			else if (tryStatement.getFinallyClause() != null) {
				CompositeStatementObject finnalyClause = tryStatement.getFinallyClause();
				if (finnalyClause.getLocationInfo().getStartLine() == lineNumber || finnalyClause.getLocationInfo().getStartLine() == lineNumber + 1) {
					return block.getComposite().getLocationInfo().getStartLine() <= lineNumber;
				}
			}
		}
		if (block.getComposite().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			CompositeStatementObject ifComp = (CompositeStatementObject)block.getComposite();
			if (ifComp.getStatements().size() == 2) {
				// if statement has an else branch, line number is the closing bracket before else branch
				AbstractStatement ifBranch = ifComp.getStatements().get(0);
				AbstractStatement elseBranch = ifComp.getStatements().get(1);
				if(ifBranch.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
						ifBranch.getLocationInfo().getEndLine() == lineNumber &&
						elseBranch.getLocationInfo().getStartLine() == lineNumber + 1) {
					return block.getComposite().getLocationInfo().getStartLine() <= lineNumber;
				}
			}
		}
	    return block.getComposite().getLocationInfo().getStartLine() <= lineNumber &&
	            block.getComposite().getLocationInfo().getEndLine() == lineNumber;
	}

	protected boolean startLineElseBlockPredicate(Block block) {
		// in case of if-else-if chain, this method should return true only for the first if in the chain
		// we assume the first if in the chain, introduced the else
		if (block.getComposite().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			CompositeStatementObject ifComp = (CompositeStatementObject)block.getComposite();
			CompositeStatementObject parent = ifComp.getParent();
			if (parent != null && !parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				// find the last else-if in the chain
				while (ifComp.getStatements().size() == 2 && ifComp.getStatements().get(1).getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
					ifComp = (CompositeStatementObject) ifComp.getStatements().get(1);
				}
				if (ifComp.getStatements().size() == 2) {
					// lineNumber is else branch
					AbstractStatement ifBranch = ifComp.getStatements().get(0);
					AbstractStatement elseBranch = ifComp.getStatements().get(1);
					if (ifBranch.getLocationInfo().getEndLine() == lineNumber &&
							elseBranch.getLocationInfo().getStartLine() == lineNumber &&
							!elseBranch.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						return block.getComposite().getLocationInfo().getStartLine() <= lineNumber;
					}
					if (ifBranch.getLocationInfo().getEndLine() == lineNumber - 1 &&
							elseBranch.getLocationInfo().getStartLine() == lineNumber &&
							!elseBranch.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						return block.getComposite().getLocationInfo().getStartLine() <= lineNumber;
					}
				}
			}
		}
		return false;
	}

	protected boolean endLineElseBlockPredicate(Block block) {
		// in case of if-else-if chain, this method should return true only for the first if in the chain
		// we assume the first if in the chain, introduced the else
		if (block.getComposite().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			CompositeStatementObject ifComp = (CompositeStatementObject)block.getComposite();
			CompositeStatementObject parent = ifComp.getParent();
			if (parent != null && !parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				// find the last else-if in the chain
				while (ifComp.getStatements().size() == 2 && ifComp.getStatements().get(1).getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
					ifComp = (CompositeStatementObject) ifComp.getStatements().get(1);
				}
				if (ifComp.getStatements().size() == 2) {
					// lineNumber is else branch
					AbstractStatement elseBranch = ifComp.getStatements().get(1);
					if (elseBranch.getLocationInfo().getEndLine() == lineNumber &&
							!elseBranch.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						return block.getComposite().getLocationInfo().getStartLine() <= lineNumber;
					}
				}
			}
		}
		return false;
	}

	private static Method getMethod(Version version, Predicate<Method> predicate, List<UMLOperation> operations) {
	    for (UMLOperation umlOperation : operations) {
	        Method method = Method.of(umlOperation, version);
	        if (predicate.test(method))
	            return method;
	    }
	    return null;
	}

	protected static Attribute getAttribute(UMLModel umlModel, Version version, String filePath, Predicate<Attribute> predicate) {
	    if (umlModel != null)
	        for (UMLClass umlClass : umlModel.getClassList()) {
	        	if (umlClass.getSourceFile().equals(filePath)) {
		            for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
		                Attribute attribute = getAttribute(version, predicate, anonymousClass.getAttributes());
		                if (attribute != null) return attribute;
		                attribute = getAttribute(version, predicate, anonymousClass.getEnumConstants());
		                if (attribute != null) return attribute;
		            }
		            Attribute attribute = getAttribute(version, predicate, umlClass.getAttributes());
		            if (attribute != null) return attribute;
		            attribute = getAttribute(version, predicate, umlClass.getEnumConstants());
		            if (attribute != null) return attribute;
	        	}
	        }
	    return null;
	}

	private static Attribute getAttribute(Version version, Predicate<Attribute> predicate, List<? extends UMLAttribute> attributes) {
	    for (UMLAttribute umlAttribute : attributes) {
	        Attribute attribute = Attribute.of(umlAttribute, version);
	        if (predicate.test(attribute))
	            return attribute;
	    }
	    return null;
	}

	protected static Class getClass(UMLModel umlModel, Version version, String filePath, Predicate<Class> predicate) {
	    if (umlModel != null)
	        for (UMLClass umlClass : umlModel.getClassList()) {
	        	if (umlClass.getSourceFile().equals(filePath)) {
		            Class clazz = Class.of(umlClass, version);
		            if (predicate.test(clazz))
		                return clazz;
	        	}
	        }
	    return null;
	}

	protected static Import getImport(UMLModel umlModel, Version version, String filePath, Predicate<Import> predicate) {
	    if (umlModel != null)
	        for (UMLClass umlClass : umlModel.getClassList()) {
	        	if (umlClass.getSourceFile().equals(filePath) && umlClass.isTopLevel()) {
		            for (UMLImport umlImport : umlClass.getImportedTypes()) {
		        		Import imp = Import.of(umlImport, umlClass, version);
			            if (predicate.test(imp))
			                return imp;
		            }
	        	}
	        }
	    return null;
	}

	protected static Package getPackage(UMLModel umlModel, Version version, String filePath, Predicate<Package> predicate) {
	    if (umlModel != null)
	        for (UMLClass umlClass : umlModel.getClassList()) {
	        	if (umlClass.getSourceFile().equals(filePath)) {
		            if (umlClass.getPackageDeclaration().isPresent()) {
		            	UMLPackage umlPackage = umlClass.getPackageDeclaration().get();
		        		Package pack = Package.of(umlPackage, umlClass, version);
			            if (predicate.test(pack))
			                return pack;
		            }
	        	}
	        }
	    return null;
	}

	protected CodeElement locateWithName(Version version, UMLModel umlModel) throws CodeElementNotFoundException {
        Class clazz = getClass(umlModel, version, filePath, this::classPredicateWithName);
        if (clazz != null) {
            return clazz;
        }
        Attribute attribute = getAttribute(umlModel, version, filePath, this::attributePredicateWithName);
        if (attribute != null) {
            return attribute;
        }
        Method method = getMethod(umlModel, version, filePath, this::methodPredicateWithName);
        if (method != null) {
            return method;
        }
        else {
            method = getMethod(umlModel, version, filePath, this::methodPredicateWithoutName);
            if (method != null) {
                Variable variable = method.findVariable(this::variablePredicate);
                if (variable != null) {
                    return variable;
                }
                Block block = method.findBlock(this::blockPredicate);
                if (block != null) {
                    return block;
                }
            }
        }
        throw new CodeElementNotFoundException(filePath, name, lineNumber);
	}

	public CodeElement locateWithoutName(Version version, UMLModel umlModel) throws CodeElementNotFoundException {
        Method method = getMethod(umlModel, version, filePath, this::methodPredicateWithoutName);
        if (method != null) {
            Block block = method.findBlockWithoutName(this::blockPredicate);
            if (block != null) {
                return block;
            }
            else {
            	block = method.findBlockWithoutName(this::startLineElseBlockPredicate);
            	if (block != null) {
            		block.checkElseBlockStart(lineNumber);
            		return block;
            	}
            	block = method.findBlockWithoutName(this::endLineElseBlockPredicate);
            	if (block != null) {
            		block.checkElseBlockEnd(lineNumber);
            		return block;
            	}
            	block = method.findBlockWithoutName(this::endLineBlockPredicate);
            	if (block != null) {
            		block.checkClosingBracket(lineNumber);
                    return block;
                }
            }
            Comment comment = method.findComment(this::commentPredicate);
            if (comment != null) {
            	return comment;
            }
            Annotation annotation = method.findAnnotation(this::annotationPredicate);
            if (annotation != null) {
            	return annotation;
            }
            Attribute attribute = getAttribute(umlModel, version, filePath, this::attributePredicateWithoutName);
            if (attribute != null && method.getLocation().subsumes(attribute.getLocation())) {
            	return attribute;
            }
            method.checkClosingBracket(lineNumber);
            return method;
        }
        Attribute attribute = getAttribute(umlModel, version, filePath, this::attributePredicateWithoutName);
        if (attribute != null) {
        	Block block = attribute.findBlockWithoutName(this::blockPredicate);
            if (block != null) {
                return block;
            }
            else {
            	block = attribute.findBlockWithoutName(this::startLineElseBlockPredicate);
            	if (block != null) {
            		block.checkElseBlockStart(lineNumber);
            		return block;
            	}
            	block = attribute.findBlockWithoutName(this::endLineElseBlockPredicate);
            	if (block != null) {
            		block.checkElseBlockEnd(lineNumber);
            		return block;
            	}
            	block = attribute.findBlockWithoutName(this::endLineBlockPredicate);
            	if (block != null) {
            		block.checkClosingBracket(lineNumber);
                    return block;
                }
            }
            Comment comment = attribute.findComment(this::commentPredicate);
            if (comment != null) {
            	return comment;
            }
            Annotation annotation = attribute.findAnnotation(this::annotationPredicate);
            if (annotation != null) {
            	return annotation;
            }
            return attribute;
        }
        Import imp = getImport(umlModel, version, filePath, this::importPredicate);
        if (imp != null) {
        	return imp;
        }
        Class clazz = getClass(umlModel, version, filePath, this::classPredicateWithoutName);
        if (clazz != null) {
        	Comment comment = clazz.findComment(this::commentPredicate);
            if (comment != null) {
            	return comment;
            }
            Annotation annotation = clazz.findAnnotation(this::annotationPredicate);
            if (annotation != null) {
            	return annotation;
            }
        	clazz.checkClosingBracket(lineNumber);
        	return clazz;
        }
        Package pack = getPackage(umlModel, version, filePath, this::packagePredicate);
        if (pack != null) {
        	return pack;
        }
        throw new CodeElementNotFoundException(filePath, name, lineNumber);
	}
}