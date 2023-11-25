package org.codetracker.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import org.codetracker.rest.changeHistory.RESTChange;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.codetracker.api.*;
import org.codetracker.change.Change;
import org.codetracker.element.Block;
import org.codetracker.element.Variable;
import org.codetracker.util.CodeElementLocator;
import org.codetracker.util.GitRepository;
import org.codetracker.util.IRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import static org.codetracker.rest.REST.*;

public class RESTHandler implements HttpHandler {

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    String path = exchange.getRequestPath();

    switch (path) {
      case "/api/track":
        handleTrackRequest(exchange);
        break;
      case "/api/codeElementType":
        handleCodeElementTypeRequest(exchange);
        break;
      case "/api/addToOracle":
        handleAddToOracleRequest(exchange);
        break;
      case "/api/getOracleData":
        handleGetOracleData(exchange);
        break;
      // Add more cases for other endpoints as needed
      default:
        exchange.setStatusCode(404);
        exchange.getResponseSender().send("Endpoint not found");
    }
  }

  public static void handleTrackRequest(HttpServerExchange exchange) {
    if (exchange.getRequestMethod().equalToString("OPTIONS")) {
      // Ignore preflight request
      return;
    }

    Map<String, Deque<String>> params = exchange.getQueryParameters();

    String owner = params.get("owner").getFirst();
    String repoName = params.get("repoName").getFirst();
    String commitId = params.get("commitId").getFirst();
    String filePath = params.get("filePath").getFirst();
    String name = params.get("selection").getFirst();
    String latestCommitHash;
    String gitHubToken = null;

    try {
      gitHubToken = params.get("gitHubToken").getFirst();
    } catch (Exception ignored) {}

    int lineNumber = Integer.parseInt(params.get("lineNumber").getFirst());

    ArrayList<RESTChange> changes = null;

    GitService gitService = new GitServiceImpl();

    CredentialsProvider credentialsProvider = getCredentialsProvider(
      owner,
      gitHubToken
    );

    try (
      Repository repository = gitService.cloneIfNotExists(
        "tmp/" + owner + "/" + repoName,
        "https://github.com/" + owner + "/" + repoName + ".git",
        owner,
        gitHubToken
      )
    ) {
      try (Git git = new Git(repository)) {
        PullResult call;
        if (credentialsProvider != null) {
          call = git.pull().setCredentialsProvider(credentialsProvider).call();
        } else {
          call = git.pull().call();
        }
        System.out.println("Pulled from the remote repository: " + call);
        latestCommitHash =
          git.log().setMaxCount(1).call().iterator().next().getName();
      }
      if ("master".equals(commitId) || "main".equals(commitId)) {
        commitId = latestCommitHash;
      }

      CodeElementLocator locator = new CodeElementLocator(
        repository,
        commitId,
        filePath,
        name,
        lineNumber
      );
      CodeElement codeElement = locator.locate();

      if (codeElement == null) {
        throw new Exception("Selected code element is invalid.");
      }
      Map<String, Object> jsonMap = new HashMap<>();

      jsonMap.put("repositoryName", owner);
      jsonMap.put(
        "repositoryWebURL",
        "https://github.com/" + owner + "/" + repoName + ".git"
      );
      jsonMap.put("filePath", filePath);
      jsonMap.put("selectionType", codeElement.getClass().getSimpleName());
      jsonMap.put(
        "codeElementType",
        codeElement.getLocation().getCodeElementType()
      );

      if (codeElement.getClass() == Variable.class) {
        Variable variable = (Variable) codeElement;

        jsonMap.put("functionName", variable.getOperation().getName());
        jsonMap.put(
          "functionStartLine",
          variable.getOperation().getLocationInfo().getStartLine()
        );
        jsonMap.put("variableKey", variable.getName());
        jsonMap.put("variableStartLine", variable.getLocation().getStartLine());
      } else if (codeElement.getClass() == Block.class) {
        Block block = (Block) codeElement;

        jsonMap.put("functionName", block.getOperation().getName());
        jsonMap.put(
          "functionStartLine",
          block.getOperation().getLocationInfo().getStartLine()
        );
        jsonMap.put("blockKey", codeElement.getName());
        jsonMap.put("blockStartLine", codeElement.getLocation().getStartLine());
        jsonMap.put("blockEndLine", codeElement.getLocation().getEndLine());
      }

      changes =
        trackCodeHistory(
          repository,
          filePath,
          commitId,
          name,
          lineNumber,
          codeElement
        );

      jsonMap.put("startCommitId", commitId);
      jsonMap.put("changes", changes);

      ObjectMapper objectMapper = new ObjectMapper();
      String response = objectMapper.writeValueAsString(jsonMap);

      exchange
        .getResponseHeaders()
        .put(new HttpString("Access-Control-Allow-Origin"), "*")
        .put(Headers.CONTENT_TYPE, "text/plain");
      exchange.getResponseSender().send(response);
    } catch (Exception e) {
      System.out.println("Something went wrong: " + e);
    }
  }

  public static void handleCodeElementTypeRequest(HttpServerExchange exchange) {
    if (exchange.getRequestMethod().equalToString("OPTIONS")) {
      // Ignore preflight request
      return;
    }
    exchange
      .getResponseHeaders()
      .put(new HttpString("Access-Control-Allow-Origin"), "*")
      .put(Headers.CONTENT_TYPE, "text/plain");

    Map<String, Deque<String>> params = exchange.getQueryParameters();
    String owner = params.get("owner").getFirst();
    String repoName = params.get("repoName").getFirst();
    String commitId = params.get("commitId").getFirst();
    String filePath = params.get("filePath").getFirst();
    String name = params.get("selection").getFirst();
    String latestCommitHash;
    String gitHubToken = null;

    try {
      gitHubToken = params.get("gitHubToken").getFirst();
    } catch (Exception ignored) {}

    int lineNumber = Integer.parseInt(params.get("lineNumber").getFirst());

    GitService gitService = new GitServiceImpl();
    CredentialsProvider credentialsProvider = getCredentialsProvider(
      owner,
      gitHubToken
    );

    Map<String, Object> jsonMap = new HashMap<>();

    try (
      Repository repository = gitService.cloneIfNotExists(
        "tmp/" + owner + "/" + repoName,
        "https://github.com/" + owner + "/" + repoName + ".git",
        owner,
        gitHubToken
      )
    ) {
      try (Git git = new Git(repository)) {
        PullResult call;
        if (credentialsProvider != null) {
          call = git.pull().setCredentialsProvider(credentialsProvider).call();
        } else {
          call = git.pull().call();
        }
        System.out.println("Pulled from the remote repository: " + call);
        latestCommitHash =
          git.log().setMaxCount(1).call().iterator().next().getName();
      }
      if ("master".equals(commitId) || "main".equals(commitId)) {
        commitId = latestCommitHash;
      }

      CodeElementLocator locator = new CodeElementLocator(
        repository,
        commitId,
        filePath,
        name,
        lineNumber
      );
      CodeElement codeElement = locator.locate();

      if (codeElement == null) {
        throw new Exception("Selected code element is invalid.");
      }

      String elementType;

      switch (codeElement.getClass().getSimpleName()) {
        case "Method":
          elementType = "Method";
          break;
        case "Variable":
          elementType = "Variable";
          break;
        case "Attribute":
          elementType = "Attribute";
          break;
        case "Block":
          elementType = "Block";
          break;
        default:
          elementType = "Invalid Element";
          jsonMap.put("error", "Unsupported code element");
          break;
      }

      jsonMap.put("type", elementType);

      ObjectMapper objectMapper = new ObjectMapper();
      String response = objectMapper.writeValueAsString(jsonMap);

      exchange.getResponseSender().send(response);
    } catch (TransportException e) {
      System.out.println("Authentication Failure: " + e);
      exchange
        .getResponseSender()
        .send(
          "{\"type\": \"Invalid Element\", \"error\": \"Authentication required\"}"
        );
    } catch (CodeElementNotFoundException e) {
      System.out.println("Code element not found: " + e);
      exchange
        .getResponseSender()
        .send(
          "{\"type\": \"Invalid Element\", \"error\": \"Unsupported code element\"}"
        );
    } catch (Exception e) {
      System.out.println("Something went wrong: " + e);
      e.printStackTrace();
      exchange
        .getResponseSender()
        .send(
          "{\"type\": \"Invalid Element\", \"error\": \"Internal server error\"}"
        );
    }
  }

  public static void handleAddToOracleRequest(HttpServerExchange exchange) {
    if (exchange.getRequestMethod().equalToString("OPTIONS")) {
      // Ignore preflight request
      return;
    }
    String oracleElementType = "block";
    Map<String, Deque<String>> params = exchange.getQueryParameters();
    String commitId = params.get("commitId").getFirst();
    String commitURL = params.get("commitURL").getFirst();
    String diffKey = params.get("diffKey").getFirst();
    boolean report = Boolean.parseBoolean(params.get("report").getFirst());
    boolean valid = Boolean.parseBoolean(params.get("valid").getFirst());
    try {
      try {
        File dir;
        if (checkReported) {
          dir =
            new File(
              "src/main/resources/oracle/" +
              oracleElementType +
              "/training/invalid/test-reported"
            );
        } else {
          dir =
            new File(
              "src/main/resources/oracle/" +
              oracleElementType +
              "/training/false"
            );
        }
        File[] files = dir.listFiles(
          new FileFilter() {
            boolean first = true;

            public boolean accept(final File pathname) {
              if (first) {
                first = false;
                return true;
              }
              return false;
            }
          }
        );

        assert files != null;
        String fileName = files[0].getName();

        String folderName = valid ? "valid" : "invalid";

        File currentFile = files[0];

        if (!valid & report) {
          // create GitHub issue
          try {
            String data = FileUtils.readFileToString(files[0], "UTF-8");

            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> blockJSON = mapper.readValue(
              data,
              new TypeReference<Map<String, Object>>() {}
            );
            ArrayList<Map<String, Object>> expectedChanges = (ArrayList<Map<String, Object>>) blockJSON.get(
              "expectedChanges"
            );
            List<Map<String, Object>> changes = expectedChanges
              .stream()
              .filter(change -> change.get("commitId").equals(commitId))
              .collect(Collectors.toList());

            String changeType = (String) changes
              .stream()
              .map(change -> change.get("changeType"))
              .reduce(
                "",
                (partialString, change) ->
                  (String) partialString + change + ", "
              );
            changeType = changeType.substring(0, changeType.length() - 2);
            String elementNameAfter = (String) changes
              .get(0)
              .get("elementNameAfter");
            String elementFileAfter = (String) changes
              .get(0)
              .get("elementFileAfter");

            String token = System.getenv("GITHUB_KEY");
            GitHub gitHub = new GitHubBuilder().withOAuthToken(token).build();
            GHRepository repository = gitHub.getRepository(
                issueRepo
            );
            GHIssueBuilder issue = repository.createIssue(
              "Invalid Change History - " + elementNameAfter
            );
            issue.body(
              String.format(
                "In commit [`%s`](%s) and file `%s`, element `%s` was identified as being `%s`, which may not be accurate. " +
                System.lineSeparator() +
                System.lineSeparator() +
                "Oracle filename: `%s`",
                commitId,
                commitURL + "?diff=split#" + diffKey,
                elementFileAfter,
                elementNameAfter,
                changeType,
                fileName
              )
            );
            issue.create();
            System.out.println(
              "Issue created on repo " + issueRepo + ": " + fileName
            );
          } catch (Exception e) {
            System.out.println("Bug report failed!" + e);
          }
        }

        String newFileName =
          "src/main/resources/oracle/" +
          oracleElementType +
          "/training/" +
          folderName +
          "/" +
          fileName;

        if (!valid & report) {
          newFileName =
            "src/main/resources/oracle/" +
            oracleElementType +
            "/training/" +
            folderName +
            "/reported/" +
            fileName;
        }

        currentFile.renameTo(new File(newFileName));

        System.out.println("File moved to " + folderName + ": " + fileName);
      } catch (Exception e) {
        e.getStackTrace();
        System.out.println("Something went wrong: " + e);
      }

      exchange
        .getResponseHeaders()
        .put(new HttpString("Access-Control-Allow-Origin"), "*")
        .put(Headers.CONTENT_TYPE, "text/plain");
      exchange.getResponseSender().send("{\"success\": true}");
    } catch (Exception e) {
      System.out.println("Something went wrong: " + e);
      exchange
        .getResponseHeaders()
        .put(new HttpString("Access-Control-Allow-Origin"), "*")
        .put(Headers.CONTENT_TYPE, "text/plain");
      exchange
        .getResponseSender()
        .send(
          "{\"type\": \"Invalid Element\", \"error\": \"Internal server error\"}"
        );
    }
  }

  public static void handleGetOracleData(HttpServerExchange exchange) {
    if (exchange.getRequestMethod().equalToString("OPTIONS")) {
      // Ignore preflight request
      return;
    }
    String oracleElementType = "block";
    try {
      File dir;
      if (checkReported) {
        dir =
          new File(
            "src/main/resources/oracle/" +
            oracleElementType +
            "/training/invalid/reported"
          );
      } else {
        dir =
          new File(
            "src/main/resources/oracle/" +
            oracleElementType +
            "/training/false"
          );
      }

      File[] files = dir.listFiles();

      assert files != null;

      File firstFile = Arrays.stream(files)
              .findFirst()
              .orElse(null);

      assert firstFile != null;

      System.out.println("Evaluating: " + firstFile);
      String response = FileUtils.readFileToString(
              firstFile,
        StandardCharsets.UTF_8
      );

      exchange
        .getResponseHeaders()
        .put(new HttpString("Access-Control-Allow-Origin"), "*")
        .put(Headers.CONTENT_TYPE, "text/plain");
      exchange.getResponseSender().send(response);
    } catch (Exception e) {
      System.out.println("Something went wrong: " + e);
      exchange
        .getResponseHeaders()
        .put(new HttpString("Access-Control-Allow-Origin"), "*")
        .put(Headers.CONTENT_TYPE, "text/plain");
      exchange.getResponseSender().send("No elements found");
    }
  }

  static ArrayList<RESTChange> trackCodeHistory(
    Repository repository,
    String filePath,
    String commitId,
    String name,
    Integer lineNumber,
    CodeElement codeElement
  ) {
    ArrayList<RESTChange> changeLog = new ArrayList<>();
    try {
      History<?> history = null;
      switch (codeElement.getClass().getSimpleName()) {
        case "Method":
          MethodTracker methodTracker = CodeTracker
            .methodTracker()
            .repository(repository)
            .filePath(filePath)
            .startCommitId(commitId)
            .methodName(name)
            .methodDeclarationLineNumber(lineNumber)
            .build();
          history = methodTracker.track();
          break;
        case "Variable":
          Variable variable = (Variable) codeElement;
          VariableTracker variableTracker = CodeTracker
            .variableTracker()
            .repository(repository)
            .filePath(filePath)
            .startCommitId(commitId)
            .methodName(variable.getOperation().getName())
            .methodDeclarationLineNumber(
              variable.getOperation().getLocationInfo().getStartLine()
            )
            .variableName(name)
            .variableDeclarationLineNumber(lineNumber)
            .build();
          history = variableTracker.track();
          break;
        case "Attribute":
          AttributeTracker attributeTracker = CodeTracker
            .attributeTracker()
            .repository(repository)
            .filePath(filePath)
            .startCommitId(commitId)
            .attributeName(name)
            .attributeDeclarationLineNumber(lineNumber)
            .build();
          history = attributeTracker.track();
          break;
        case "Block":
          Block block = (Block) codeElement;
          BlockTracker blockTracker = CodeTracker
            .blockTracker()
            .repository(repository)
            .filePath(filePath)
            .startCommitId(commitId)
            .methodName(block.getOperation().getName())
            .methodDeclarationLineNumber(
              block.getOperation().getLocationInfo().getStartLine()
            )
            .codeElementType(codeElement.getLocation().getCodeElementType())
            .blockStartLineNumber(codeElement.getLocation().getStartLine())
            .blockEndLineNumber(codeElement.getLocation().getEndLine())
            .build();
          history = blockTracker.track();
          break;
        default:
          break;
      }

      assert history != null;

      for (History.HistoryInfo<?> historyInfo : history.getHistoryInfoList()) {
        ArrayList<String> currentChanges = new ArrayList<>();
        CodeElement evolutionHook = null;
        boolean evolutionPresent = false;
        System.out.println(
          "======================================================"
        );
        System.out.println("Commit ID: " + historyInfo.getCommitId());
        System.out.println(
          "Date: " +
          LocalDateTime.ofEpochSecond(
            historyInfo.getCommitTime(),
            0,
            ZoneOffset.UTC
          )
        );
        System.out.println(
          "Before: " + historyInfo.getElementBefore().getName()
        );
        System.out.println("After: " + historyInfo.getElementAfter().getName());
        for (Change change : historyInfo.getChangeList()) {
          System.out.println(change.getType().getTitle() + ": " + change);
          if (
            change.getType().getTitle().equals(change.toString().toLowerCase())
          ) {
            currentChanges.add(
              WordUtils.capitalizeFully(change.getType().getTitle())
            );
          } else {
            currentChanges.add(
              WordUtils.capitalizeFully(change.getType().getTitle()) +
              ": " +
              change
            );
          }
          evolutionPresent = change.getEvolutionHook().isPresent();
          if (evolutionPresent) {
            evolutionHook = change.getEvolutionHook().get().getElementBefore();
          }
        }
        IRepository gitRepository = new GitRepository(repository);
        RESTChange currentElement = new RESTChange(
          historyInfo.getCommitId(),
          gitRepository.getParentId(historyInfo.getCommitId()),
          LocalDateTime
            .ofEpochSecond(historyInfo.getCommitTime(), 0, ZoneOffset.UTC)
            .toString(),
          historyInfo.getElementBefore(),
          historyInfo.getElementAfter(),
          historyInfo.getCommitterName(),
          historyInfo.getCommitTime(),
          currentChanges,
          evolutionPresent,
          evolutionHook
        );
        changeLog.add(currentElement);
      }
      System.out.println(
        "======================================================"
      );
      ObjectMapper mapper = new ObjectMapper();
      mapper.setVisibility(
        PropertyAccessor.FIELD,
        JsonAutoDetect.Visibility.ANY
      );
      Collections.reverse(changeLog);
      return changeLog;
    } catch (Exception e) {
      System.out.println("Something went wrong: " + e);
    }
    return null;
  }

  public static CredentialsProvider getCredentialsProvider() {
    boolean credentialsProvided =
      System.getenv("GITHUB_USERNAME") != null &&
      System.getenv("GITHUB_KEY") != null;

    if (!credentialsProvided) {
      return null;
    }

    System.out.println(
      "Credentials: " +
      System.getenv("GITHUB_USERNAME") +
      " " +
      System.getenv("GITHUB_KEY")
    );

    return new UsernamePasswordCredentialsProvider(
      System.getenv("GITHUB_USERNAME"),
      System.getenv("GITHUB_KEY")
    );
  }

  public static CredentialsProvider getCredentialsProvider(
    String username,
    String password
  ) {
    if (username == null || password == null) {
      return getCredentialsProvider();
    }

    return new UsernamePasswordCredentialsProvider(username, password);
  }
}
