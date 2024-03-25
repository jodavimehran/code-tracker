package org.codetracker.util;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.codetracker.api.CodeElement;
import org.codetracker.element.Attribute;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.codetracker.element.Variable;
import org.codetracker.experiment.AbstractExperimentStarter;
import org.codetracker.experiment.MethodExperimentStarter;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Method.class);
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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Attribute.class);
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
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), org.codetracker.element.Class.class);
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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), org.codetracker.element.Class.class);
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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), org.codetracker.element.Class.class);
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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Attribute.class);
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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Method.class);
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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Attribute.class);
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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Attribute.class);
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
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Method.class);
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
    public void testForLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "for";
        final int lineNumber = 387;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testNesterForLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "for";
        final int lineNumber = 391;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testNestedIfLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "if";
        final int lineNumber = 389;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testIfLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "if";
        final int lineNumber = 396;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testIfLocator2() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Main.java";
        final String commitId = "b1b49751d38af0bf2476aea1f4595283615ab7de";
        final String name = "if";
        final int lineNumber = 231;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testTryLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "try";
        final int lineNumber = 317;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testIfInsideCatchLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "if";
        final int lineNumber = 331;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testForInsideTryLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "for";
        final int lineNumber = 319;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testElseIfLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "if";
        final int lineNumber = 471;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }

    @Test
    public void testIfInsideAnonymousLocator() throws Exception {
        GitService gitService = new GitServiceImpl();
        final String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/gui/TreeTable.java";
        final String commitId = "119fd4fb33bef9f5c66fc950396669af842c21a3";
        final String name = "if";
        final int lineNumber = 119;
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")){
            CodeElementLocator locator = new CodeElementLocator(repository, commitId, filePath, name, lineNumber);
            CodeElement codeElement = locator.locate();
            assertNotNull(codeElement);
            assertEquals(codeElement.getClass(), Block.class);
            assertEquals(codeElement.getLocation().getStartLine(), lineNumber);
        }
    }



    private static final String generatedFilePathTemplate = "experiments/tracking-accuracy/%s/tracker/final.csv";
    private static final String expectedFilePathTemplate = "src/test/resources/tracking-accuracy/%s/tracker/final.csv";

    public static Stream<Arguments> experimentStartedProvider() {
        return Stream.of(
//                Arguments.of(new AttributeExperimentStarter(), "attribute"),
                Arguments.of(new MethodExperimentStarter(), "method")
//                Arguments.of(new ClassExperimentStarter(), "class"),
//                Arguments.of(new VariableExperimentStarter(), "variable"),
//                Arguments.of(new BlockExperimentStarter(), "block")
        );
    }

    @Test
    public void c(){
        System.out.println("a");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource(value = "experimentStartedProvider")
    public void ExperimentStarterTest(AbstractExperimentStarter abstractExperimentStarter, String folderName) throws IOException, CsvException {
        String expectedFilePath = String.format(expectedFilePathTemplate, folderName);
        String generatedFilePath = String.format(generatedFilePathTemplate, folderName);
        System.out.println(expectedFilePath);
        System.out.println(generatedFilePath);
//        abstractExperimentStarter.start();
        try (
                CSVReader expectedCSV = new CSVReader(new FileReader(expectedFilePath));
                CSVReader actualCSV = new CSVReader(new FileReader(generatedFilePath))
        ) {
            List<String[]> expectedLines = expectedCSV.readAll();
            List<String[]> actualLines = actualCSV.readAll();

            for (int i = 0; i < actualLines.size() && i < expectedLines.size(); i++) {
                String[] record1 = actualLines.get(i);
                String[] record2 = expectedLines.get(i);
                String cellCsv1_col6 = record1[6];
                String cellCsv1_col7 = record1[7];
                String cellCsv1_col8 = record1[8];
                String cellCsv2_col6 = record2[6];
                String cellCsv2_col7 = record2[7];
                String cellCsv2_col8 = record2[8];
                System.out.println("Comparing " + cellCsv1_col6 + " with " + cellCsv2_col6);
                System.out.println("Comparing " + cellCsv1_col7 + " with " + cellCsv2_col8);
                System.out.println("Comparing " + cellCsv1_col7 + " with " + cellCsv2_col8);
                assertEquals(cellCsv1_col6, cellCsv2_col6);
                assertEquals(cellCsv1_col7, cellCsv2_col7);
                assertEquals(cellCsv1_col8, cellCsv2_col8);
            }
        }
    }
}
