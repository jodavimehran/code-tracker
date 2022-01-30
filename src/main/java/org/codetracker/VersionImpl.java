package org.codetracker;

import org.codetracker.api.Version;

import java.util.Objects;

public class VersionImpl implements Version {
  private final String id;
  private final long time;
  private final String authorName;

  public VersionImpl(String id, long time, String authorName) {
    this.id = id;
    this.time = time;
    this.authorName = authorName;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public long getTime() {
    return this.time;
  }

  @Override
  public String getAuthorName() {
    return authorName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VersionImpl version = (VersionImpl) o;
    return Objects.equals(id, version.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return String.format("<%s>", getId());
  }
}
