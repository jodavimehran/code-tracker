package org.codetracker.util;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.HashMap;

public class GitRepository implements IRepository {
  private final Repository repository;
  private final HashMap<String, RevCommit> revCommitCache = new HashMap<>();

  public GitRepository(Repository repository) {
    this.repository = repository;
  }

  protected RevCommit getRevCommit(String commitId) {
    RevCommit revCommit = null;
    try {
      revCommitCache.putIfAbsent(commitId, repository.parseCommit(ObjectId.fromString(commitId)));
      revCommit = revCommitCache.get(commitId);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return revCommit;
  }

  @Override
  public String getParentId(String commitId) {
    RevCommit revCommit = getRevCommit(commitId);
    if (revCommit.getParentCount() > 0) return revCommit.getParent(0).getId().getName();
    return "0";
  }

  @Override
  public long getCommitTime(String commitId) {
    if ("0".equals(commitId)) return 0;
    return getRevCommit(commitId).getCommitTime();
  }

  @Override
  public long getAuthoredTime(String commitId) {
    if ("0".equals(commitId)) return 0;
    // convert time to Unix epoch
    return getRevCommit(commitId).getAuthorIdent().getWhen().getTime() / 1000L;
  }

  @Override
  public String getCommitAuthorName(String commitId) {
    if ("0".equals(commitId)) return "";
    return getRevCommit(commitId).getAuthorIdent().getName();
  }
}
