<h1>Code Tracker</h1>

This project aims to introduce CodeTracker, a refactoring-aware tool that can generate the commit change history for method and variable declarations in a Java project with a very high accuracy.

# Table of Contents

  * [How to Build](#how-to-build)
  * [How to add as a Maven dependency](#how-to-add-as-a-maven-dependency)
  * [How to Track Methods](#how-to-track-methods)
  * [How to Track Variables](#how-to-track-variables)
  * [Oracle](#oracle)
  * [Execution Time Experiment](#execution-time-experiment)


# How to Build
To build this project you need to have Gradle and Maven.

Download CodeTracker and RefactoringMiner from [here](https://drive.google.com/file/d/1XzrvwVXA-Agy66JoviFqpkoCJBkcSDtt/view?usp=sharing).
1. **Build RefactoringMiner**: Run `gradle publishToMavenLocal` in the root folder of RefactoringMiner.
2. **Build CodeTracker**: Run `mvn install` int the root folder of CodeTracker.

# How to add as a Maven dependency

To add code-tracker as a maven dependency in your project, add the following dependency:

    <dependency>
      <groupId>org.codetracker</groupId>
      <artifactId>code-tracker</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>

# How to Track Methods
CodeTracker can track the history of methods in the git repositories.

In the code snippet below we demonstrate how to print all changes performed in the history of [`public void fireErrors(String fileName, SortedSet<LocalizedMessage> errors)`](https://github.com/checkstyle/checkstyle/blob/119fd4fb33bef9f5c66fc950396669af842c21a3/src/main/java/com/puppycrawl/tools/checkstyle/Checker.java#L384).

```java
    GitService gitService = new GitServiceImpl();
    try (Repository repository = gitService.cloneIfNotExists("tmp/checkstyle", "https://github.com/checkstyle/checkstyle.git")){

        MethodTracker methodTracker = CodeTracker.methodTracker()
            .repository(repository)
            .filePath("src/main/java/com/puppycrawl/tools/checkstyle/Checker.java")
            .startCommitId("119fd4fb33bef9f5c66fc950396669af842c21a3")
            .methodName("fireErrors")
            .methodDeclarationLineNumber(384)
            .build();
     
        History<Method> methodHistory = methodTracker.track();

        for (EndpointPair<Method> edge : methodHistory.getGraph().getEdges()) {
            Edge edgeValue = methodHistory.getGraph().getEdgeValue(edge).get();
            for (Change change : edgeValue.getChangeList()) {
                if (Change.Type.NO_CHANGE.equals(change.getType()))
                    continue;
                String commitId = edge.target().getVersion().getId();
                String changeType = change.getType().getTitle();
                String changeDescription = change.toString();
                System.out.printf("%s,%s,%s%n", commitId, changeType, change);
            }
        }
    }
```

# How to Track Variables
CodeTracker can track the history of variables in the git repositories.

In the code snippet below we demonstrate how to print all changes performed in the history of [`final String stripped`](https://github.com/checkstyle/checkstyle/blob/119fd4fb33bef9f5c66fc950396669af842c21a3/src/main/java/com/puppycrawl/tools/checkstyle/Checker.java#L385).

```java
    GitService gitService = new GitServiceImpl();
    try (Repository repository = gitService.cloneIfNotExists("tmp/checkstyle", "https://github.com/checkstyle/checkstyle.git")){

        VariableTracker variableTracker = CodeTracker.variableTracker()
            .repository(repository)
            .filePath("src/main/java/com/puppycrawl/tools/checkstyle/Checker.java")
            .startCommitId("119fd4fb33bef9f5c66fc950396669af842c21a3")
            .methodName("fireErrors")
            .methodDeclarationLineNumber(384)
            .variableName("stripped")
            .variableDeclarationLineNumber(385)
            .build();
        History<Variable> variableHistory = variableTracker.track();
     
        for (EndpointPair<Variable> edge : variableHistory.getGraph().getEdges()) {
            Edge edgeValue = variableHistory.getGraph().getEdgeValue(edge).get();
            for (Change change : edgeValue.getChangeList()) {
                if (Change.Type.NO_CHANGE.equals(change.getType()))
                    continue;
                String commitId = edge.target().getVersion().getId();
                String changeType = change.getType().getTitle();
                String changeDescription = change.toString();
                System.out.printf("%s,%s,%s%n", commitId, changeType, change);
            }
        }
    }
```

# Oracle
The oracle we used to evaluate CodeTracker is an extension of [CodeShovel oracle](https://github.com/ataraxie/codeshovel/tree/master/src/test/resources/oracles/java), including the evolution history of 200 methods and the evolution history of 1346 variables declared in these methods, is available in the following links:
* [Method](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle)
  * [Training](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training)
  * [Test](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/test)
* [Variable](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/variable)
  * [Training](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/variable/training)
  * [Test](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/variable/test)

In the extended oracle we fixed all inaccuracies that we found in the original oracle. For example, the following methods in the original oracle are erroneously matched with another method which is extracted from their body. In fact, these methods are *introduced* as a result of an Extract Method refactoring.
* Training
  * [checkstyle-CommonUtils-createPattern](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/checkstyle-CommonUtils-createPattern.json)
  * [checkstyle-WhitespaceAroundCheck-shouldCheckSeparationFromNextToken](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/checkstyle-WhitespaceAroundCheck-shouldCheckSeparationFromNextToken.json)
  * [checkstyle-WhitespaceAroundCheck-isNotRelevantSituation](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/checkstyle-WhitespaceAroundCheck-isNotRelevantSituation.json)
  * [commons-lang-EqualsBuilder-reflectionAppend](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/commons-lang-EqualsBuilder-reflectionAppend.json)
  * [commons-lang-RandomStringUtils-random](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/commons-lang-RandomStringUtils-random.json)
  * [commons-lang-NumberUtils-isCreatable](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/commons-lang-NumberUtils-isCreatable.json)
  * [flink-FileSystem-getUnguardedFileSystem](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/flink-FileSystem-getUnguardedFileSystem.json)
  * [flink-RemoteStreamEnvironment-executeRemotely](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/flink-RemoteStreamEnvironment-executeRemotely.json)
  * [hibernate-orm-SimpleValue-buildAttributeConverterTypeAdapter](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/hibernate-orm-SimpleValue-buildAttributeConverterTypeAdapter.json)
  * [javaparser-MethodResolutionLogic-isApplicable](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/javaparser-MethodResolutionLogic-isApplicable.json)
  * [javaparser-Difference-applyRemovedDiffElement](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/javaparser-Difference-applyRemovedDiffElement.json)
  * [javaparser-JavaParserFacade-getTypeConcrete](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/javaparser-JavaParserFacade-getTypeConcrete.json)
  * [jgit-IndexDiff-diff](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/jgit-IndexDiff-diff.json)
  * [jgit-UploadPack-sendPack](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/jgit-UploadPack-sendPack.json)
  * [junit4-ParentRunner-applyValidators](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/junit4-ParentRunner-applyValidators.json)
  * [junit5-TestMethodTestDescriptor-invokeTestMethod](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/junit5-TestMethodTestDescriptor-invokeTestMethod.json)
  * [junit5-DefaultLauncher-discoverRoot](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/junit5-DefaultLauncher-discoverRoot.json)
  * [okhttp-Http2Connection-newStream](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/training/okhttp-Http2Connection-newStream.json)
* Test
  * [commons-io-IOUtils-toInputStream](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/test/commons-io-IOUtils-toInputStream.json)
  * [commons-io-FilenameUtils-wildcardMatch](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/test/commons-io-FilenameUtils-wildcardMatch.json)
  * [hadoop-SchedulerApplicationAttempt-resetSchedulingOpportunities](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/test/hadoop-SchedulerApplicationAttempt-resetSchedulingOpportunities.json)
  * [hibernate-search-ClassLoaderHelper-instanceFromName](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/test/hibernate-search-ClassLoaderHelper-instanceFromName.json)
  * [spring-boot-DefaultErrorAttributes-addErrorMessage](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/test/spring-boot-DefaultErrorAttributes-addErrorMessage.json)
  * [lucene-solr-QueryParserBase-addClause](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/test/lucene-solr-QueryParserBase-addClause.json)
  * [intellij-community-ModuleCompileScope-isUrlUnderRoot](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/test/intellij-community-ModuleCompileScope-isUrlUnderRoot.json)
  * [intellij-community-TranslatingCompilerFilesMonitor-isInContentOfOpenedProject](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/test/intellij-community-TranslatingCompilerFilesMonitor-isInContentOfOpenedProject.json)
  * [mockito-MatchersBinder-bindMatchers](https://github.com/jodavimehran/refactoring-refiner/tree/master/src/main/resources/history/method/oracle/test/mockito-MatchersBinder-bindMatchers.json)

# Execution Time Experiment
As part of our experiments, we measured the execution time of CodeTracker and CodeShovel to track each method's change history in the training and testing sets. All data we recorded for this experiment and the script for generating the execution time plots are available [here](https://github.com/jodavimehran/refactoring-refiner/tree/master/experiments/execution-time).
 
