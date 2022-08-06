package org.codetracker.util;

import org.codetracker.api.CodeElement;
import org.codetracker.element.Attribute;
import org.codetracker.element.Method;
import org.codetracker.element.Variable;
import org.eclipse.jgit.lib.Repository;
import org.junit.Assert;
import org.junit.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            Assert.assertNotNull(codeElement);
            Assert.assertEquals(codeElement.getClass(), Method.class);
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
            Assert.assertNotNull(codeElement);
            Assert.assertEquals(codeElement.getClass(), Variable.class);
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
            Assert.assertNotNull(codeElement);
            Assert.assertEquals(codeElement.getClass(), Variable.class);
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
            Assert.assertNotNull(codeElement);
            Assert.assertEquals(codeElement.getClass(), Variable.class);
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
            Assert.assertNotNull(codeElement);
            Assert.assertEquals(codeElement.getClass(), Variable.class);
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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            Assert.assertNotNull(codeElement);
            Assert.assertEquals(codeElement.getClass(), Attribute.class);
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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            Assert.assertNotNull(codeElement);
            Assert.assertEquals(codeElement.getClass(), org.codetracker.element.Class.class);
        }
    }

    @Test
    public void testInnerClassLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/ConfigurationLoader.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "IgnoredModulesOptions";
        final int lineNumber = 58;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            Assert.assertNotNull(codeElement);
            Assert.assertEquals(codeElement.getClass(), org.codetracker.element.Class.class);
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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            Assert.assertNotNull(codeElement);
            Assert.assertEquals(codeElement.getClass(), Attribute.class);
        }
    }
}
