<h1>Refactoring Refiner</h1>

<p>
This project aims to introduce Refactoring-Refiner, a JAVA API to refine the atomic refactoring activities mined by [RefactoringMiner](https://github.com/tsantalis/RefactoringMiner) in the historyImpl of a Java project.

</p>

# Table of Contents

  * [How to Build](#how-to-build)
  * [How to add as a Maven dependency](#how-to-add-as-a-maven-dependency)

# How to Build

To build the project, run `mvn package`.

# How to add as a Maven dependency

To add refactoring-refiner as a maven dependency in a project, run `maven install` to add `refactoring-refiner-1.0-SNAPSHOT.jar` into your local mvn repository, and then add the following dependency to your project:

    <dependency>
      <groupId>org.refactoringrefiner</groupId>
      <artifactId>refactoring-refiner</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
