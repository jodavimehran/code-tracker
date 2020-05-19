# Table of Contents

  * [General Info](#general-info)
  * [How to Build](#how-to-build)
  * [How to add as a Maven dependency](#how-to-add-as-a-maven-dependency)

# General Info
refactoring-refiner is a JAVA API based on [RefactoringMiner](https://github.com/tsantalis/RefactoringMiner) to refine the atomic refactoring activities mined in the history of a Java project.

# How to Build

To build the project, run `mvn package`. Hense this project uses RefactoringMiner as a maven dependency, it is really important to add `RefactoringMiner.jar` into your local mvn repository. If you don't have RefactoringMiner, see [this](https://github.com/tsantalis/RefactoringMiner#how-to-use-refactoringminer-as-a-maven-dependency) section.

# How to add as a Maven dependency

To add refactoring-refiner as a maven dependency in a project, run `maven install` to add `refactoring-refiner-1.0-SNAPSHOT.jar` into your local mvn repository, and then add the following dependency to your project:
      
      <dependency>
            <groupId>org.refactoringrefiner</groupId>
            <artifactId>refactoring-refiner</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
