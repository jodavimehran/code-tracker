<h1>Code Tracker</h1>

This project aims to introduce CodeTracker, a refactoring-aware tool that can generate the commit change history for method and variable declarations in a Java project with a very high accuracy.

# Table of Contents
  * [How to cite CodeTracker](#how-to-cite-codetracker)
  * [Requirements](#requirements)
  * [How to Build and Run](#how-to-build-and-run)
      * [Command line](#command-line)
      * [Eclipse IDE](#eclipse-ide)
      * [IntelliJ IDEA](#intellij-idea)
  * [How to add as a Maven dependency](#how-to-add-as-a-maven-dependency)
  * [Refactoring-aware Blame](#refactoring-aware-blame)
      * [How to blame a single line](#how-to-blame-a-single-line)
      * [How to blame an entire file](#how-to-blame-an-entire-file)
  * [How to Track Blocks](#how-to-track-blocks)
  * [How to Track Methods](#how-to-track-methods)
  * [How to Track Variables](#how-to-track-variables)
  * [How to Track Attributes](#how-to-track-attributes)
  * [How to Run the REST API](#how-to-run-the-rest-api)
  * [REST API Endpoints](#rest-api-endpoints)
  * [Oracle](#oracle)
  * [Experiments](#experiments)

# How to cite CodeTracker
If you are using CodeTracker in your research, please cite the following papers:

Mehran Jodavi and Nikolaos Tsantalis, "[Accurate Method and Variable Tracking in Commit History](https://users.encs.concordia.ca/~nikolaos/publications/FSE_2022.pdf)," pp. 183-195, *30th ACM Joint European Software Engineering Conference and Symposium on the Foundations of Software Engineering* (ESEC/FSE'2022), Singapore, Singapore, November 14–18, 2022.

    @inproceedings{10.1145/3540250.3549079,
       author = {Jodavi, Mehran and Tsantalis, Nikolaos},
	   title = {Accurate Method and Variable Tracking in Commit History},
	   year = {2022},
	   isbn = {9781450394130},
	   publisher = {Association for Computing Machinery},
	   address = {New York, NY, USA},
	   url = {https://doi.org/10.1145/3540250.3549079},
	   doi = {10.1145/3540250.3549079},
	   booktitle = {Proceedings of the 30th ACM Joint European Software Engineering Conference and Symposium on the Foundations of Software Engineering},
	   pages = {183–195},
	   numpages = {13},
	   keywords = {commit change history, refactoring-aware source code tracking},
	   location = {Singapore, Singapore},
	   series = {ESEC/FSE 2022}
    }

Mohammed Tayeeb Hasan, Nikolaos Tsantalis, and Pouria Alikhanifard, "[Refactoring-aware Block Tracking in Commit History](https://users.encs.concordia.ca/~nikolaos/publications/TSE_2024.pdf)," *IEEE Transactions on Software Engineering*, 2024.

    @article{Hasan:TSE:2024:CodeTracker2.0,
       author = {Hasan, Mohammed Tayeeb and Tsantalis, Nikolaos and Alikhanifard, Pouria},
	   journal = {IEEE Transactions on Software Engineering},
	   title = {Refactoring-aware Block Tracking in Commit History},
	   year = {2024},
	   pages = {1-20},
	   doi = {10.1109/TSE.2024.3484586}
    }

# Requirements
Java 11.0.15 or newer

Apache Maven 3.6.3 or newer

# How to Build and Run
## Command line
1. **Clone repository**

`git clone https://github.com/jodavimehran/code-tracker.git`

2. **Cd in the locally cloned repository folder**

`cd code-tracker`

3. **Build code-tracker**

`mvn install`

4. **Run the API usage examples shown in README**

`mvn compile exec:java -Dexec.mainClass="org.codetracker.Main"`

Note: by default the repository https://github.com/checkstyle/checkstyle.git will be cloned in folder "code-tracker/tmp".
If you want to change folder where the repository will be cloned, you have to edit the field `FOLDER_TO_CLONE` in class `org.codetracker.Main`
and execute `mvn install` again

5. **Run the method tracking experiment** (takes around 20 minutes for 200 tracked methods)

`mvn compile exec:java -Dexec.mainClass="org.codetracker.experiment.MethodExperimentStarter"`

6. **Run the variable tracking experiment** (takes around 2 hours for 1345 tracked variables)

`mvn compile exec:java -Dexec.mainClass="org.codetracker.experiment.VariableExperimentStarter"`

7. **Run the block tracking experiment** (takes around 2 hours for 1280 tracked blocks)

`mvn compile exec:java -Dexec.mainClass="org.codetracker.experiment.BlockExperimentStarter"`

Note: by default the analyzed repositories will be cloned in folder "code-tracker/tmp".
If you want to change folder where the repositories will be cloned, you have to edit the field `FOLDER_TO_CLONE` in class `org.codetracker.experiment.AbstractExperimentStarter`
and execute `mvn install` again 

## Eclipse IDE
1. **Clone repository**

`git clone https://github.com/jodavimehran/code-tracker.git`

2. **Import project**

Go to *File* -> *Import...* -> *Maven* -> *Existing Maven Projects*

*Browse* to the root directory of project code-tracker

Click *Finish*

The project will be built automatically.

3. **Run the API usage examples shown in README**

From the Package Explorer navigate to `org.codetracker.Main`

Right-click on the file and select *Run as* -> *Java Application*

4. **Run the method tracking experiment** (takes around 20 minutes for 200 tracked methods)

From the Package Explorer navigate to `org.codetracker.experiment.MethodExperimentStarter`

Right-click on the file and select *Run as* -> *Java Application*

5. **Run the variable tracking experiment** (takes around 2 hours for 1345 tracked variables)

From the Package Explorer navigate to `org.codetracker.experiment.VariableExperimentStarter`

Right-click on the file and select *Run as* -> *Java Application*

6. **Run the block tracking experiment** (takes around 2 hours for 1280 tracked blocks)

From the Package Explorer navigate to `org.codetracker.experiment.BlockExperimentStarter`

Right-click on the file and select *Run as* -> *Java Application*

## IntelliJ IDEA
1. **Clone repository**

`git clone https://github.com/jodavimehran/code-tracker.git`

2. **Import project**

Go to *File* -> *Open...*

Browse to the root directory of project code-tracker

Click *OK*

The project will be built automatically.

3. **Run the API usage examples shown in README**

From the Project tab navigate to `org.codetracker.Main`

Right-click on the file and select *Run Main.main()*

4. **Run the method tracking experiment** (takes around 20 minutes for 200 tracked methods)

From the Project tab navigate to `org.codetracker.experiment.MethodExperimentStarter`

Right-click on the file and select *Run MethodExperimentStarter.main()*

5. **Run the variable tracking experiment** (takes around 2 hours for 1345 tracked variables)

From the Project tab navigate to `org.codetracker.experiment.VariableExperimentStarter`

Right-click on the file and select *Run VariableExperimentStarter.main()*

6. **Run the block tracking experiment** (takes around 2 hours for 1280 tracked blocks)

From the Project tab navigate to `org.codetracker.experiment.BlockExperimentStarter`

Right-click on the file and select *Run BlockExperimentStarter.main()*


# How to add as a Maven dependency

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.jodavimehran/code-tracker/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.jodavimehran/code-tracker)

Since version 1.0, CodeTracker is available in the [Maven Central Repository](https://mvnrepository.com/artifact/io.github.jodavimehran/code-tracker).
In order to use CodeTracker as a maven dependency in your project, add the following snippet to your project's build configuration file:

**pom.xml**

    <dependency>
      <groupId>io.github.jodavimehran</groupId>
      <artifactId>code-tracker</artifactId>
      <version>3.0</version>
    </dependency>

**build.gradle**

    implementation 'io.github.jodavimehran:code-tracker:3.0'

# Refactoring-aware Blame

## How to blame a single line
```java
import static org.codetracker.blame.util.Utils.getOwner;
import static org.codetracker.blame.util.Utils.getProject;

// REPOS_PATH is the directory where the repository is locally cloned
String url = "https://github.com/checkstyle/checkstyle/commit/119fd4fb33bef9f5c66fc950396669af842c21a3";
String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
int lineNumber = 69;
String commitId = URLHelper.getCommit(url);
Repository repository = new GitServiceImpl().cloneIfNotExists(REPOS_PATH + "/" + getOwner(url) + "/" + getProject(url), URLHelper.getRepo(url));

History.HistoryInfo<? extends CodeElement> lineBlame =
	new CodeTrackerBlame().getLineBlame(repository, commitId, filePath, lineNumber);
LineBlameResult lineBlameResult = LineBlameResult.of(lineBlame, lineNumber);
```

## How to blame an entire file
```java
import static org.codetracker.blame.util.Utils.getOwner;
import static org.codetracker.blame.util.Utils.getProject;

// REPOS_PATH is the directory where the repository is locally cloned
String url = "https://github.com/checkstyle/checkstyle/commit/119fd4fb33bef9f5c66fc950396669af842c21a3";
String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
String commitId = URLHelper.getCommit(url);
Repository repository = new GitServiceImpl().cloneIfNotExists(REPOS_PATH + "/" + getOwner(url) + "/" + getProject(url), URLHelper.getRepo(url));

FileTrackerImpl fileTracker = new FileTrackerImpl(repository, commitId, filePath);
fileTracker.blame();
BlameFormatter blameFormatter = new BlameFormatter(fileTracker.getLines());
List<String[]> results = blameFormatter.make(fileTracker.getBlameInfo());
String actual = TabularPrint.make(results);
```

# How to Track Blocks
CodeTracker can track the history of code blocks in git repositories.

In the code snippet below we demonstrate how to print all changes performed in the history of [`for (final AuditListener listener : listeners)`](https://github.com/checkstyle/checkstyle/blob/119fd4fb33bef9f5c66fc950396669af842c21a3/src/main/java/com/puppycrawl/tools/checkstyle/Checker.java#L391).

`.codeElementType()` can take the following values:
- `CodeElementType.FOR_STATEMENT`
- `CodeElementType.ENHANCED_FOR_STATEMENT`
- `CodeElementType.WHILE_STATEMENT`
- `CodeElementType.IF_STATEMENT`
- `CodeElementType.DO_STATEMENT`
- `CodeElementType.SWITCH_STATEMENT`
- `CodeElementType.SYNCHRONIZED_STATEMENT`
- `CodeElementType.TRY_STATEMENT`
- `CodeElementType.CATCH_CLAUSE`
- `CodeElementType.FINALLY_BLOCK`

```java
    GitService gitService = new GitServiceImpl();
    try (Repository repository = gitService.cloneIfNotExists("tmp/checkstyle",
            "https://github.com/checkstyle/checkstyle.git")){

        BlockTracker blockTracker = CodeTracker.blockTracker()
                .repository(repository)
                .filePath("src/main/java/com/puppycrawl/tools/checkstyle/Checker.java")
                .startCommitId("119fd4fb33bef9f5c66fc950396669af842c21a3")
                .methodName("fireErrors")
                .methodDeclarationLineNumber(384)
                .codeElementType(CodeElementType.ENHANCED_FOR_STATEMENT)
                .blockStartLineNumber(391)
                .blockEndLineNumber(393)
                .build();

        History<Block> blockHistory = blockTracker.track();

        for (History.HistoryInfo<Block> historyInfo : blockHistory.getHistoryInfoList()) {
            System.out.println("======================================================");
            System.out.println("Commit ID: " + historyInfo.getCommitId());
            System.out.println("Date: " +
                    LocalDateTime.ofEpochSecond(historyInfo.getCommitTime(), 0, ZoneOffset.UTC));
            System.out.println("Before: " + historyInfo.getElementBefore().getName());
            System.out.println("After: " + historyInfo.getElementAfter().getName());

            for (Change change : historyInfo.getChangeList()) {
                System.out.println(change.getType().getTitle() + ": " + change);
            }
        }
        System.out.println("======================================================");
    }
```

# How to Track Methods
CodeTracker can track the history of methods in git repositories.

In the code snippet below we demonstrate how to print all changes performed in the history of [`public void fireErrors(String fileName, SortedSet<LocalizedMessage> errors)`](https://github.com/checkstyle/checkstyle/blob/119fd4fb33bef9f5c66fc950396669af842c21a3/src/main/java/com/puppycrawl/tools/checkstyle/Checker.java#L384).

```java
    GitService gitService = new GitServiceImpl();
    try (Repository repository = gitService.cloneIfNotExists("tmp/checkstyle",
            "https://github.com/checkstyle/checkstyle.git")){

        MethodTracker methodTracker = CodeTracker.methodTracker()
            .repository(repository)
            .filePath("src/main/java/com/puppycrawl/tools/checkstyle/Checker.java")
            .startCommitId("119fd4fb33bef9f5c66fc950396669af842c21a3")
            .methodName("fireErrors")
            .methodDeclarationLineNumber(384)
            .build();
     
        History<Method> methodHistory = methodTracker.track();

        for (History.HistoryInfo<Method> historyInfo : methodHistory.getHistoryInfoList()) {
            System.out.println("======================================================");
            System.out.println("Commit ID: " + historyInfo.getCommitId());
            System.out.println("Date: " + 
                LocalDateTime.ofEpochSecond(historyInfo.getCommitTime(), 0, ZoneOffset.UTC));
            System.out.println("Before: " + historyInfo.getElementBefore().getName());
            System.out.println("After: " + historyInfo.getElementAfter().getName());
            
            for (Change change : historyInfo.getChangeList()) {
                System.out.println(change.getType().getTitle() + ": " + change);
            }
        }
        System.out.println("======================================================");
    }
```

# How to Track Variables
CodeTracker can track the history of variables in git repositories.

In the code snippet below we demonstrate how to print all changes performed in the history of [`final String stripped`](https://github.com/checkstyle/checkstyle/blob/119fd4fb33bef9f5c66fc950396669af842c21a3/src/main/java/com/puppycrawl/tools/checkstyle/Checker.java#L385).

```java
    GitService gitService = new GitServiceImpl();
    try (Repository repository = gitService.cloneIfNotExists("tmp/checkstyle",
            "https://github.com/checkstyle/checkstyle.git")){

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

        for (History.HistoryInfo<Variable> historyInfo : variableHistory.getHistoryInfoList()) {
            System.out.println("======================================================");
            System.out.println("Commit ID: " + historyInfo.getCommitId());
            System.out.println("Date: " + 
                LocalDateTime.ofEpochSecond(historyInfo.getCommitTime(), 0, ZoneOffset.UTC));
            System.out.println("Before: " + historyInfo.getElementBefore().getName());
            System.out.println("After: " + historyInfo.getElementAfter().getName());
            
            for (Change change : historyInfo.getChangeList()) {
                System.out.println(change.getType().getTitle() + ": " + change);
            }
        }
        System.out.println("======================================================");
    }
```
# How to Track Attributes
CodeTracker can track the history of attributes in git repositories.

In the code snippet below we demonstrate how to print all changes performed in the history of [`private PropertyCacheFile cacheFile`](https://github.com/checkstyle/checkstyle/blob/119fd4fb33bef9f5c66fc950396669af842c21a3/src/main/java/com/puppycrawl/tools/checkstyle/Checker.java#L132).

```java
    GitService gitService = new GitServiceImpl();
    try (Repository repository = gitService.cloneIfNotExists("tmp/checkstyle",
            "https://github.com/checkstyle/checkstyle.git")) {

        AttributeTracker attributeTracker = CodeTracker.attributeTracker()
                .repository(repository)
                .filePath("src/main/java/com/puppycrawl/tools/checkstyle/Checker.java")
                .startCommitId("119fd4fb33bef9f5c66fc950396669af842c21a3")
                .attributeName("cacheFile")
                .attributeDeclarationLineNumber(132)
                .build();

        History<Attribute> attributeHistory = attributeTracker.track();

        for (History.HistoryInfo<Attribute> historyInfo : attributeHistory.getHistoryInfoList()) {
            System.out.println("======================================================");
            System.out.println("Commit ID: " + historyInfo.getCommitId());
            System.out.println("Date: " + 
                LocalDateTime.ofEpochSecond(historyInfo.getCommitTime(), 0, ZoneOffset.UTC));
            System.out.println("Before: " + historyInfo.getElementBefore().getName());
            System.out.println("After: " + historyInfo.getElementAfter().getName());

            for (Change change : historyInfo.getChangeList()) {
                System.out.println(change.getType().getTitle() + ": " + change);
            }
        }
        System.out.println("======================================================");
    }
```

# How to Run the REST API

You can serve CodeTracker as a REST API. 

In the command line, run

`mvn compile exec:java -Dexec.mainClass="org.codetracker.rest.REST"`

To provide GitHub credentials for tracking private repositories, set environment variables `GITHUB_USERNAME` and `GITHUB_KEY` before running the API.
 - `set GITHUB_USERNAME=<your_username>`
 - `set GITHUB_KEY=<your_github_key>`

# Rest API Endpoints

### Endpoint

`HTTP Method`: `GET`

`Endpoint URL`: `/api/track`

#### Endpoint Description

Initiate one of the four supported Trackers on a given code element. Returns the change history of the selected element in the form of a JSON array. Works for all types of supported code elements (methods, attributes, variables, blocks).

### Parameters

#### Request Parameters (query params)

| Parameter     | Type     | Description                                                     |
|---------------|----------|-----------------------------------------------------------------|
| `owner`       | `String` | The owner of the repository.                                    |
| `repoName`    | `String` | The name of the repository.                                     |
| `commitId`    | `String` | The commit Id to start tracking from.                           |
| `filePath`    | `String` | The path of the file the code element is defined in.            |
| `selection`   | `String` | The code element to be tracked.                                 |
| `lineNumber`  | `String` | The line the code element is defined on                         |
| `gitHubToken` | `String` | [Optional] The GitHub access token for private repositories.    |

### Request Example

```json
{
    "owner": "checkstyle",
    "repoName": "checkstyle",
    "filePath": "src/main/java/com/puppycrawl/tools/checkstyle/JavadocDetailNodeParser.java",
    "commitId": "119fd4fb33bef9f5c66fc950396669af842c21a3",
    "selection": "stack",
    "lineNumber": "486"
}
```

### Endpoint

`HTTP Method`: `GET`

`Endpoint URL`: `/api/codeElementType`

#### Endpoint Description

Detect the type of code element selected using the `CodeElementLocator` API. Returns the type of code element selected. Works for all types of supported code elements (methods, attributes, variables, blocks).

### Parameters

#### Request Parameters (query params)

| Parameter     | Type     | Description                                                     |
|---------------|----------|-----------------------------------------------------------------|
| `owner`       | `String` | The owner of the repository.                                    |
| `repoName`    | `String` | The name of the repository.                                     |
| `commitId`    | `String` | The commit Id to start tracking from.                           |
| `filePath`    | `String` | The path of the file the code element is defined in.            |
| `selection`   | `String` | The code element to be tracked.                                 |
| `lineNumber`  | `String` | The line the code element is defined on                         |
| `gitHubToken` | `String` | [Optional] The GitHub access token for private repositories.    |

### Request Example

```json
{
    "owner": "checkstyle",
    "repoName": "checkstyle",
    "filePath": "src/main/java/com/puppycrawl/tools/checkstyle/JavadocDetailNodeParser.java",
    "commitId": "119fd4fb33bef9f5c66fc950396669af842c21a3",
    "selection": "stack",
    "lineNumber": "486"
}
```

# Oracle
The oracle we used to evaluate CodeTracker is an extension of [CodeShovel oracle](https://github.com/ataraxie/codeshovel/tree/master/src/test/resources/oracles/java), including the evolution history of 200 methods and the evolution history of **1345 variables** and **1280 blocks** declared in these methods, is available in the following links:
* [Method](src/main/resources/oracle/method)
  * [Training](src/main/resources/oracle/method/training)
  * [Test](src/main/resources/oracle/method/test)
* [Block](src/main/resources/oracle/block)
  * [Training](src/main/resources/oracle/block/training)
  * [Test](src/main/resources/oracle/block/test)
* [Variable](src/main/resources/oracle/variable)
  * [Training](src/main/resources/oracle/variable/training)
  * [Test](src/main/resources/oracle/variable/test)

### JSON property descriptions
**repositoryName**: folder in which the repository is cloned  
**repositoryWebURL**: Git repository URL  
**filePath**: file path in the start commit  
**functionName**: method declaration name in the start commit  
**functionKey**: unique string key of the method declaration in the start commit  
**functionStartLine**: method declaration start line in the start commit  
**variableName**: variable declaration name in the start commit  
**variableKey**: unique string key of the variable declaration in the start commit  
**variableStartLine**: variable declaration start line in the start commit  
**startCommitId**: start commit SHA-1  
**expectedChanges**: list of changes on the tracked program element in the commit history of the project  
**parentCommitId**: parent commit SHA-1  
**commitId**: child commit SHA-1  
**commitTime**: commit time in Unix epoch (or Unix time or POSIX time or Unix timestamp) format  
**changeType**: type change  
**elementFileBefore**: file path in the parent commit  
**elementNameBefore**: unique string key of the program element in the parent commit  
**elementFileAfter**: file path in the child commit  
**elementNameAfter**: unique string key of the program element in the child commit  
**comment**: Refactoring or change description

### Some Samples of CodeShovel's false cases
In the extended oracle we fixed all inaccuracies that we found in the original oracle. For example, the following methods in the original oracle are erroneously matched with another method which is extracted from their body. In fact, these methods are *introduced* as a result of an Extract Method refactoring.
* Training
  * [checkstyle-CommonUtils-createPattern](src/main/resources/oracle/method/training/checkstyle-CommonUtils-createPattern.json)
  * [checkstyle-WhitespaceAroundCheck-shouldCheckSeparationFromNextToken](src/main/resources/oracle/method/training/checkstyle-WhitespaceAroundCheck-shouldCheckSeparationFromNextToken.json)
  * [checkstyle-WhitespaceAroundCheck-isNotRelevantSituation](src/main/resources/oracle/method/training/checkstyle-WhitespaceAroundCheck-isNotRelevantSituation.json)
  * [commons-lang-EqualsBuilder-reflectionAppend](src/main/resources/oracle/method/training/commons-lang-EqualsBuilder-reflectionAppend.json)
  * [commons-lang-RandomStringUtils-random](src/main/resources/oracle/method/training/commons-lang-RandomStringUtils-random.json)
  * [commons-lang-NumberUtils-isCreatable](src/main/resources/oracle/method/training/commons-lang-NumberUtils-isCreatable.json)
  * [flink-FileSystem-getUnguardedFileSystem](src/main/resources/oracle/method/training/flink-FileSystem-getUnguardedFileSystem.json)
  * [flink-RemoteStreamEnvironment-executeRemotely](src/main/resources/oracle/method/training/flink-RemoteStreamEnvironment-executeRemotely.json)
  * [hibernate-orm-SimpleValue-buildAttributeConverterTypeAdapter](src/main/resources/oracle/method/training/hibernate-orm-SimpleValue-buildAttributeConverterTypeAdapter.json)
  * [javaparser-MethodResolutionLogic-isApplicable](src/main/resources/oracle/method/training/javaparser-MethodResolutionLogic-isApplicable.json)
  * [javaparser-Difference-applyRemovedDiffElement](src/main/resources/oracle/method/training/javaparser-Difference-applyRemovedDiffElement.json)
  * [javaparser-JavaParserFacade-getTypeConcrete](src/main/resources/oracle/method/training/javaparser-JavaParserFacade-getTypeConcrete.json)
  * [jgit-IndexDiff-diff](src/main/resources/oracle/method/training/jgit-IndexDiff-diff.json)
  * [jgit-UploadPack-sendPack](src/main/resources/oracle/method/training/jgit-UploadPack-sendPack.json)
  * [junit4-ParentRunner-applyValidators](src/main/resources/oracle/method/training/junit4-ParentRunner-applyValidators.json)
  * [junit5-TestMethodTestDescriptor-invokeTestMethod](src/main/resources/oracle/method/training/junit5-TestMethodTestDescriptor-invokeTestMethod.json)
  * [junit5-DefaultLauncher-discoverRoot](src/main/resources/oracle/method/training/junit5-DefaultLauncher-discoverRoot.json)
  * [okhttp-Http2Connection-newStream](src/main/resources/oracle/method/training/okhttp-Http2Connection-newStream.json)
* Test
  * [commons-io-IOUtils-toInputStream](src/main/resources/oracle/method/test/commons-io-IOUtils-toInputStream.json)
  * [commons-io-FilenameUtils-wildcardMatch](src/main/resources/oracle/method/test/commons-io-FilenameUtils-wildcardMatch.json)
  * [hadoop-SchedulerApplicationAttempt-resetSchedulingOpportunities](src/main/resources/oracle/method/test/hadoop-SchedulerApplicationAttempt-resetSchedulingOpportunities.json)
  * [hibernate-search-ClassLoaderHelper-instanceFromName](src/main/resources/oracle/method/test/hibernate-search-ClassLoaderHelper-instanceFromName.json)
  * [spring-boot-DefaultErrorAttributes-addErrorMessage](src/main/resources/oracle/method/test/spring-boot-DefaultErrorAttributes-addErrorMessage.json)
  * [lucene-solr-QueryParserBase-addClause](src/main/resources/oracle/method/test/lucene-solr-QueryParserBase-addClause.json)
  * [intellij-community-ModuleCompileScope-isUrlUnderRoot](src/main/resources/oracle/method/test/intellij-community-ModuleCompileScope-isUrlUnderRoot.json)
  * [intellij-community-TranslatingCompilerFilesMonitor-isInContentOfOpenedProject](src/main/resources/oracle/method/test/intellij-community-TranslatingCompilerFilesMonitor-isInContentOfOpenedProject.json)
  * [mockito-MatchersBinder-bindMatchers](src/main/resources/oracle/method/test/mockito-MatchersBinder-bindMatchers.json)

### CodeTracker's misreporting samples
To avoid unnecessary processing and speed up the tracking process, CodeTracker excludes some files from the source code model. The excluding action may cause misreporting of change type in some special scenarios. Although CodeTracker supports three scenarios in which additional files need to be included in the source code model, it may misreport MoveMethod changes as FileMove because the child commit model did not include the origin file of the method. In the test oracle, there are three such cases: [case 1](https://github.com/jodavimehran/code-tracker/blob/master/src/main/resources/oracle/method/test/hadoop-SchedulerApplicationAttempt-resetSchedulingOpportunities.json), [case 2](https://github.com/jodavimehran/code-tracker/blob/master/src/main/resources/oracle/method/test/mockito-AdditionalMatchers-geq.json) and [case 3](https://github.com/jodavimehran/code-tracker/blob/master/src/main/resources/oracle/method/test/mockito-AdditionalMatchers-gt.json). 

# Experiments
### Execution Time:
  As part of our experiments, we measured the execution time of CodeTracker and CodeShovel to track each method's change history in the training and testing sets. All data we recorded for this experiment and the script for generating the execution time plots are available [here](experiments/execution-time).
### Tracking Accuracy
  All data we collect to compute the precision and recall of CodeTracker and CodeShovel at commit level and change level are available in the following links:
* [Method](experiments/tracking-accuracy/method)
* [Block](experiments/tracking-accuracy/block)
* [Variable](experiments/tracking-accuracy/variable)
  
### CSV column descriptions
`detailed-tracker-training.csv` `detailed-tracker-test.csv`  
**file_name**: corresponding JSON file name in the oracle  
**repository**: Git repository URL  
**element_key**: unique string key of the program element in the start commit  
**parent_commit_id**: parent commit SHA-1  
**commit_id**: child commit SHA-1  
**commit_time**: commit time in Unix epoch (or Unix time or POSIX time or Unix timestamp) format  
**change_type**: type of change  
**element_file_before**: file path in the parent commit  
**element_file_after**: file path in the child commit  
**element_name_before**: unique string key of the program element in the parent commit  
**element_name_after**: unique string key of the program element in the child commit  
**result**: True Positive (TP), False Positive (FP) or False Negative (FN)  
**comment**: Refactoring or change description  

`summary-tracker-training.csv` `summary-tracker-test.csv`  
**instance**: unique string key of the program element in the start commit  
**processing_time**: total execution time in milliseconds  
**analysed_commits**: total number of processed commits  
**git_log_command_calls**: number of times git log command was executed (step 1 of our approach)  
**step2**: number of times step 2 of our approach was executed  
**step3**: number of times step 3 of our approach was executed  
**step4**: number of times step 4 of our approach was executed  
**step5**: number of times step 5 of our approach was executed  
**tp_change_type**: number of True Positives (TP) for this specific `change_type`  
**fp_change_type**: number of False Positives (FP) for this specific `change_type`  
**fn_change_type**: number of False Negatives (FN) for this specific `change_type`  
**tp_all**: total number of True Positives (TP)  
**fp_all**: total number of False Positives (FP)  
**fn_all**: total number of False Negatives (FN)  

`final.csv`  
**tool**: tool name (tracker or shovel)  
**oracle**: oracle name (training or test)  
**level**: change report level (commit or change)  
**processing_time_avg**: average processing time  
**processing_time_median**: median processing time  
**tp**: total number of True Positives (TP)  
**fp**: total number of False Positives (FP)  
**fn**: total number of False Negatives (FN)  
**precision**: precision percentage  
**recall**: recall percentage  
