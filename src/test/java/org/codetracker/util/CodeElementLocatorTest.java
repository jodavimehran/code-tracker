package org.codetracker.util;

import org.codetracker.api.CodeElement;
import org.codetracker.element.Attribute;
import org.codetracker.element.Block;
import org.codetracker.element.Class;
import org.codetracker.element.Comment;
import org.codetracker.element.Import;
import org.codetracker.element.Method;
import org.codetracker.element.Package;
import org.codetracker.element.Variable;
import org.eclipse.jgit.lib.Repository;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import gr.uom.java.xmi.LocationInfo.CodeElementType;

@Disabled
public class CodeElementLocatorTest {
    private final static String FOLDER_TO_CLONE = "tmp/";

    @Test
    public void testMethodLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "fireErrors";
        final int lineNumber = 384;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Method.class);
            assertEquals(((Method)codeElement).getUmlOperation().getName(), name);
        }
    }

    @Test
    public void testMethodParameterLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "errors";
        final int lineNumber = 384;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Variable.class);
        }
    }

    @Test
    public void testLocalVariableLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "stripped";
        final int lineNumber = 385;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Variable.class);
        }
    }

    @Test
    public void testLambdaParameterLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "filter";
        final int lineNumber = 246;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Variable.class);
        }
    }

    @Test
    public void testLocalVariableInsideLambdaLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "locations";
        final int lineNumber = 247;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Variable.class);
        }
    }

    @Test
    public void testCatchExceptionLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "ex";
        final int lineNumber = 183;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Variable.class);
        }
    }

    @Test
    public void testAttributeLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "classLoader";
        final int lineNumber = 93;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Attribute.class);
            assertEquals(((Attribute)codeElement).getUmlAttribute().getName(), name);
        }
    }

    @Test
    public void testClassLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "Checker";
        final int lineNumber = 67;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), org.codetracker.element.Class.class);
            assertEquals(((org.codetracker.element.Class)codeElement).getUmlClass().getNonQualifiedName(), name);
        }
    }

    @Test
    public void testInnerEnumLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "IgnoredModulesOptions";
        final int lineNumber = 58;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), org.codetracker.element.Class.class);
            assertEquals(((org.codetracker.element.Class)codeElement).getUmlClass().getNonQualifiedName(), name);
        }
    }

    @Test
    public void testInnerClassLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "InternalLoader";
        final int lineNumber = 568;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), org.codetracker.element.Class.class);
            assertEquals(((org.codetracker.element.Class)codeElement).getUmlClass().getNonQualifiedName(), name);
        }
    }

    @Test
    public void testInnerClassAttributeLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "METADATA";
        final int lineNumber = 586;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Attribute.class);
            assertEquals(((Attribute)codeElement).getUmlAttribute().getName(), name);
        }
    }

    @Test
    public void testInnerClassMethodLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "containsAttribute";
        final int lineNumber = 703;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Method.class);
            assertEquals(((Method)codeElement).getUmlOperation().getName(), name);
        }
    }

    @Test
    public void testInnerClassMethodParameterLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "module";
        final int lineNumber = 703;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Variable.class);
        }
    }

    @Test
    public void testInnerClassLocalVariableLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "result";
        final int lineNumber = 705;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Variable.class);
        }
    }

    @Test
    public void testInnerClassLambdaParameterLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "name";
        final int lineNumber = 706;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Variable.class);
        }
    }

    @Test
    public void testInnerClassCatchExceptionLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "ex";
        final int lineNumber = 675;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Variable.class);
        }
    }

    @Test
    public void testInnerClassEnumConstantLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "OMIT";
        final int lineNumber = 63;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Attribute.class);
            assertEquals(((Attribute)codeElement).getUmlAttribute().getName(), name);
        }
    }

    @Test
    public void testAnonymousClassAttributeLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/gui/TreeTable.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "serialVersionUID";
        final int lineNumber = 104;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Attribute.class);
            assertEquals(((Attribute)codeElement).getUmlAttribute().getName(), name);
        }
    }

    @Test
    public void testAnonymousClassMethodLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/gui/TreeTable.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "actionPerformed";
        final int lineNumber = 107;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Method.class);
            assertEquals(((Method)codeElement).getUmlOperation().getName(), name);
        }
    }

    @Test
    public void testAnonymousClassMethodParameterLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/gui/TreeTable.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "event";
        final int lineNumber = 107;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Variable.class);
        }
    }

    @Test
    public void testStatementLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 392;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.EXPRESSION_STATEMENT);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testForLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 387;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.ENHANCED_FOR_STATEMENT);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testNestedForLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 391;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.ENHANCED_FOR_STATEMENT);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testNestedIfLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 389;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testIfLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 396;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testIfLocator2() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Main.java";
        final String commitId = "b1b49751d38af0bf2476aea1f4595283615ab7de";
        final int lineNumber = 231;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testTryLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 317;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.TRY_STATEMENT);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testCatchLocator() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 323;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Block.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.CATCH_CLAUSE);
	        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testCatchLocator2() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 330;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Block.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.CATCH_CLAUSE);
	        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testFinallyLocator() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Main.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 229;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Block.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.FINALLY_BLOCK);
	        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testIfInsideCatchLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 331;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testForInsideTryLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 319;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.ENHANCED_FOR_STATEMENT);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testElseIfLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 471;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testIfInsideAnonymousLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/gui/TreeTable.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 119;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }
    // Closing Bracket tests
    @Test
    public void testClosingBracketElseBlockLocator() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 541;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Block.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
	        assertEquals(codeElement.getLocation().getStartLine(), 527);
	        assertTrue(((Block)codeElement).isClosingCurlyBracket());
        }
    }

    @Test
    public void testClosingBracketElseBlockLocator2() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 539;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Block.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IF_STATEMENT);
	        assertEquals(codeElement.getLocation().getStartLine(), 534);
	        assertTrue(((Block)codeElement).isClosingCurlyBracket());
        }
    }

    @Test
    public void testClosingBracketTryLocator() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 322;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
        	CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Block.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.TRY_STATEMENT);
	        assertEquals(codeElement.getLocation().getStartLine(), 317);
	        assertTrue(((Block)codeElement).isClosingCurlyBracket());
        }
    }

    @Test
    public void testClosingBracketCatchLocator() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 328;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
        	CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Block.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.CATCH_CLAUSE);
	        assertEquals(codeElement.getLocation().getStartLine(), 323);
	        assertTrue(((Block)codeElement).isClosingCurlyBracket());
        }
    }

    @Test
    public void testClosingBracketCatchLocator2() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 346;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
        	CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Block.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.CATCH_CLAUSE);
	        assertEquals(codeElement.getLocation().getStartLine(), 330);
	        assertTrue(((Block)codeElement).isClosingCurlyBracket());
        }
    }

    @Test
    public void testClosingBracketFinallyLocator() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Main.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 243;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
        	CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Block.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.FINALLY_BLOCK);
	        assertEquals(codeElement.getLocation().getStartLine(), 229);
	        assertTrue(((Block)codeElement).isClosingCurlyBracket());
        }
    }

    @Test
    public void testClosingBracketClassLocator() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 627;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
        	CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Class.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.TYPE_DECLARATION);
	        assertEquals(codeElement.getLocation().getStartLine(), 60);
	        assertTrue(((Class)codeElement).isClosingCurlyBracket());
        }
    }
    // Comment tests
    @Test
    public void testCommentInMethodLocator() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Main.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 660;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
        	CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Comment.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.LINE_COMMENT);
	        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
	        assertEquals(((Comment)codeElement).getOperation().get().getName(), "listFiles");
        }
    }

    @Test
    public void testCommentInInnerClassMethodLocator() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 607;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
        	CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Comment.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.LINE_COMMENT);
	        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
	        assertEquals(((Comment)codeElement).getOperation().get().getName(), "startElement");
        }
    }
    // Import tests
    @Test
    public void testImportLocator() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 58;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
        	CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Import.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.IMPORT_DECLARATION);
	        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
	        assertEquals(((Import)codeElement).getUmlImport().getName(), "com.puppycrawl.tools.checkstyle.utils.CommonUtils");
        }
    }
    // Package tests
    @Test
    public void testPackageLocator() throws Exception {
    	GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final int lineNumber = 20;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
        	CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, lineNumber);
	        CodeElement codeElement = locator.locate();
	        assertNotNull(codeElement);
	        assertEquals(codeElement.getClass(), Package.class);
	        assertEquals(codeElement.getLocation().getCodeElementType(), CodeElementType.PACKAGE_DECLARATION);
	        assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
	        assertEquals(((Package)codeElement).getUmlPackage().getName(), "com.puppycrawl.tools.checkstyle");
        }
    }
}
