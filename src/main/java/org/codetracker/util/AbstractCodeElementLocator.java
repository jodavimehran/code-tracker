package org.codetracker.util;

import java.util.List;
import java.util.function.Predicate;

import org.codetracker.api.CodeElement;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.Version;
import org.codetracker.element.Attribute;
import org.codetracker.element.Block;
import org.codetracker.element.Class;
import org.codetracker.element.Method;
import org.codetracker.element.Variable;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;

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
		return clazz.getUmlClass().getLocationInfo().getStartLine() <= lineNumber &&
	            clazz.getUmlClass().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean methodPredicateWithName(Method method) {
	    return method.getUmlOperation().getName().equals(name) &&
	            method.getUmlOperation().getLocationInfo().getStartLine() <= lineNumber &&
	            method.getUmlOperation().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean methodPredicateWithoutName(Method method) {
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
	    return attribute.getUmlAttribute().getLocationInfo().getStartLine() <= lineNumber &&
	            attribute.getUmlAttribute().getLocationInfo().getEndLine() >= lineNumber;
	}

	protected boolean blockPredicate(Block block) {
	    return block.getComposite().getLocationInfo().getStartLine() == lineNumber &&
	            block.getComposite().getLocationInfo().getEndLine() >= lineNumber;
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
                	checkClosingBracket(block);
                    return block;
                }
            }
        }
        throw new CodeElementNotFoundException(filePath, name, lineNumber);
	}

	protected CodeElement locateWithoutName(Version version, UMLModel umlModel) throws CodeElementNotFoundException {
        Method method = getMethod(umlModel, version, filePath, this::methodPredicateWithoutName);
        if (method != null) {
            Block block = method.findBlockWithoutName(this::blockPredicate);
            if (block != null) {
            	checkClosingBracket(block);
                return block;
            }
            Attribute attribute = getAttribute(umlModel, version, filePath, this::attributePredicateWithoutName);
            if (attribute != null) {
            	return attribute;
            }
            return method;
        }
        Attribute attribute = getAttribute(umlModel, version, filePath, this::attributePredicateWithoutName);
        if (attribute != null) {
        	Block block = attribute.findBlockWithoutName(this::blockPredicate);
            if (block != null) {
            	checkClosingBracket(block);
                return block;
            }
            return attribute;
        }
        Class clazz = getClass(umlModel, version, filePath, this::classPredicateWithoutName);
        if (clazz != null) {
        	return clazz;
        }
        throw new CodeElementNotFoundException(filePath, name, lineNumber);
	}

	private void checkClosingBracket(Block block) {
		if (block.getLocation().getEndLine() == lineNumber && block.getComposite() instanceof CompositeStatementObject) {
			block.setClosingCurlyBracket(true);
		}
	}
}