<h1>Code Tracker</h1>

This project aims to introduce Code Tracker, a refactoring-aware tool that can generate the commit change history for method and variable declarations in the history of a Java project with a very high accuracy.

# Table of Contents

  * [How to Build](#how-to-build)
  * [How to add as a Maven dependency](#how-to-add-as-a-maven-dependency)
  * [How to Track Methods](#how-to-track-methods)
  * 

# How to Build
To build this project you need to have Gradle and Maven.

Download CodeTracker and Refactoring Miner from [here](#).

First step is building Refactoring Miner. Just run `gradle publishToMavenLocal` in the root folder of Refactoring Miner.

Second and last step is building CodeTracker. Just run `mvn install` int the root folder of CodeTracker.

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