package org.codetracker.util;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

public class GitHubRepository implements IRepository {
  private final GHRepository repository;

  public GitHubRepository(GHRepository repository) {
    this.repository = repository;
  }

  @Override
  public String getParentId(String commitId) {
    String parentCommitId = "";
    try {
      GHCommit currentCommit = repository.getCommit(commitId);
      parentCommitId = currentCommit.getParents().get(0).getSHA1();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return parentCommitId;
  }

  @Override
  public long getCommitTime(String commitId) {
    long commitTime = 0;
    if ("0".equals(commitId)) return commitTime;
    try {
      GHCommit currentCommit = repository.getCommit(commitId);
      commitTime = currentCommit.getCommitDate().getTime();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return commitTime;
  }

  @Override
  public String getCommitAuthorName(String commitId) {
    if ("0".equals(commitId)) {
      return "";
    }
    try {
      GHCommit currentCommit = repository.getCommit(commitId);
      return currentCommit.getCommitter().getName();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }
}
