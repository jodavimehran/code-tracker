package org.codetracker.util;

import org.codetracker.api.CodeElement;
import org.codetracker.element.Annotation;
import org.codetracker.element.Attribute;
import org.codetracker.element.Block;
import org.codetracker.element.Class;
import org.codetracker.element.Comment;
import org.codetracker.element.Import;
import org.codetracker.element.Method;
import org.codetracker.element.Package;
import org.codetracker.element.Variable;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import gr.uom.java.xmi.LocationInfo.CodeElementType;

public class CodeElementLocatorWithLocalFilesTest {

    @Test
    public void testMethodLocator() throws Exception {
        final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "fireErrors";
        final int lineNumber = 384;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Method.class);
        assertEquals(((Method)codeElement).getUmlOperation().getName(), name);
    }

    @Test
    public void testMethodParameterLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "errors";
        final int lineNumber = 384;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, name, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Variable.class);
    }

    @Test
    public void testLocalVariableLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "stripped";
        final int lineNumber = 385;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, name, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Variable.class);
    }

    @Test
    public void testLambdaParameterLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "filter";
        final int lineNumber = 246;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, name, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Variable.class);
    }

    @Test
    public void testLocalVariableInsideLambdaLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "locations";
        final int lineNumber = 247;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, name, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Variable.class);
    }

    @Test
    public void testCatchExceptionLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "ex";
        final int lineNumber = 183;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, name, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Variable.class);
    }

    @Test
    public void testAttributeLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "classLoader";
        final int lineNumber = 93;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Attribute.class);
        assertEquals(((Attribute)codeElement).getUmlAttribute().getName(), name);
    }

    @Test
    public void testClassLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "Checker";
        final int lineNumber = 67;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), org.codetracker.element.Class.class);
        assertEquals(((org.codetracker.element.Class)codeElement).getUmlClass().getNonQualifiedName(), name);
    }

    @Test
    public void testInnerEnumLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "IgnoredModulesOptions";
        final int lineNumber = 58;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), org.codetracker.element.Class.class);
        assertEquals(((org.codetracker.element.Class)codeElement).getUmlClass().getNonQualifiedName(), name);
    }

    @Test
    public void testInnerClassLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "InternalLoader";
        final int lineNumber = 568;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), org.codetracker.element.Class.class);
        assertEquals(((org.codetracker.element.Class)codeElement).getUmlClass().getNonQualifiedName(), name);
    }

    @Test
    public void testInnerClassAttributeLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "METADATA";
        final int lineNumber = 586;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Attribute.class);
        assertEquals(((Attribute)codeElement).getUmlAttribute().getName(), name);
    }

    @Test
    public void testInnerClassMethodLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "containsAttribute";
        final int lineNumber = 703;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Method.class);
        assertEquals(((Method)codeElement).getUmlOperation().getName(), name);
    }

    @Test
    public void testInnerClassMethodParameterLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "module";
        final int lineNumber = 703;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, name, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Variable.class);
    }

    @Test
    public void testInnerClassLocalVariableLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "result";
        final int lineNumber = 705;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, name, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Variable.class);
    }

    @Test
    public void testInnerClassLambdaParameterLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "name";
        final int lineNumber = 706;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, name, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Variable.class);
    }

    @Test
    public void testInnerClassCatchExceptionLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "ex";
        final int lineNumber = 675;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, name, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Variable.class);
    }

    @Test
    public void testInnerClassEnumConstantLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "OMIT";
        final int lineNumber = 63;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Attribute.class);
        assertEquals(((Attribute)codeElement).getUmlAttribute().getName(), name);
    }

    @Test
    public void testAnonymousClassAttributeLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/gui/TreeTable.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "serialVersionUID";
        final int lineNumber = 104;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Attribute.class);
        assertEquals(((Attribute)codeElement).getUmlAttribute().getName(), name);
    }

    @Test
    public void testAnonymousClassMethodLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/gui/TreeTable.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "actionPerformed";
        final int lineNumber = 107;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Method.class);
        assertEquals(((Method)codeElement).getUmlOperation().getName(), name);
    }

    @Test
    public void testAnonymousClassMethodParameterLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/gui/TreeTable.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "event";
        final int lineNumber = 107;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, name, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Variable.class);
    }

    @Test
    public void testStatementLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 392;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.EXPRESSION_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testForLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 387;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.ENHANCED_FOR_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testNestedForLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 391;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.ENHANCED_FOR_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testNestedIfLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 389;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testIfLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 396;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testIfLocator2() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Main.java";
        final String commitId = "b1b49751d38af0bf2476aea1f4595283615ab7de";
        final int lineNumber = 231;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testTryLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 317;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.TRY_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testCatchLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 323;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.CATCH_CLAUSE);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testCatchLocator2() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 330;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.CATCH_CLAUSE);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testFinallyLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Main.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 229;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.FINALLY_BLOCK);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testIfInsideCatchLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 331;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testForInsideTryLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 319;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.ENHANCED_FOR_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testElseIfLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 471;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testIfInsideAnonymousLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/gui/TreeTable.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 119;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }
    // Closing Bracket tests
    @Test
    public void testClosingBracketElseBlockLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 541;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), 527);
        assertTrue(((Block)codeElement).isElseBlockEnd());
    }

    @Test
    public void testClosingBracketElseBlockLocator2() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 539;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), 534);
        assertTrue(((Block)codeElement).isElseBlockEnd());
    }

    @Test
    public void testClosingBracketTryLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 322;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.TRY_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), 317);
        assertTrue(((Block)codeElement).isClosingCurlyBracket());
    }

    @Test
    public void testClosingBracketCatchLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 328;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.CATCH_CLAUSE);
        assertEquals(codeElement.getLocation().getStartLine(), 323);
        assertTrue(((Block)codeElement).isClosingCurlyBracket());
    }

    @Test
    public void testClosingBracketCatchLocator2() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 346;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.CATCH_CLAUSE);
        assertEquals(codeElement.getLocation().getStartLine(), 330);
        assertTrue(((Block)codeElement).isClosingCurlyBracket());
    }

    @Test
    public void testClosingBracketFinallyLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Main.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 243;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.FINALLY_BLOCK);
        assertEquals(codeElement.getLocation().getStartLine(), 229);
        assertTrue(((Block)codeElement).isClosingCurlyBracket());
    }

    @Test
    public void testClosingBracketClassLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 627;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Class.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.TYPE_DECLARATION);
        assertEquals(codeElement.getLocation().getStartLine(), 60);
        assertTrue(((Class)codeElement).isClosingCurlyBracket());
    }
    // Comment tests
    @Test
    public void testCommentInMethodLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Main.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 660;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Comment.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.LINE_COMMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        assertEquals(((Comment)codeElement).getOperation().get().getName(), "listFiles");
    }

    @Test
    public void testCommentInInnerClassMethodLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 607;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Comment.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.LINE_COMMENT);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        assertEquals(((Comment)codeElement).getOperation().get().getName(), "startElement");
    }
    // Import tests
    @Test
    public void testImportLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 58;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Import.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IMPORT_DECLARATION);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        assertEquals(((Import)codeElement).getUmlImport().getName(), "com.puppycrawl.tools.checkstyle.utils.CommonUtils");
    }
    // Package tests
    @Test
    public void testPackageLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 20;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Package.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.PACKAGE_DECLARATION);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        assertEquals(((Package)codeElement).getUmlPackage().getName(), "com.puppycrawl.tools.checkstyle");
    }
    // Opening Else Bracket tests
    @Test
    public void testOpeningBracketElseBlockLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 530;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), 527);
        assertTrue(((Block)codeElement).isElseBlockStart());
    }

    @Test
    public void testOpeningBracketElseBlockLocator2() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 537;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Block.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
        assertEquals(codeElement.getLocation().getStartLine(), 534);
        assertTrue(((Block)codeElement).isElseBlockStart());
    }
    // Annotation tests
    @Test
    public void testMethodAnnotationLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 205;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Annotation.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.ANNOTATION);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testAnonymousMethodAnnotationLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Main.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 423;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Annotation.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.ANNOTATION);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }

    @Test
    public void testInnerClassMethodAnnotationLocator() throws Exception {
    	final String cloneURL = "https://github.com/checkstyle/checkstyle.git";
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 600;
        CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, commitId, filePath, lineNumber);
        CodeElement codeElement = locator.locate();
        assertNotNull(codeElement);
        assertEquals(codeElement.getClass(), Annotation.class);
        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.ANNOTATION);
        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
    }
}
