package org.codetracker.blame.benchmark;

import java.io.Serializable;
import java.util.Objects;

public class BlameCaseInfo implements Serializable {
    String url;
    String filePath;

    public BlameCaseInfo() {
    }

    public BlameCaseInfo(String url, String filePath) {
        this.url = url;
        this.filePath = filePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlameCaseInfo that = (BlameCaseInfo) o;

        if (!Objects.equals(url, that.url)) return false;
        return Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (filePath != null ? filePath.hashCode() : 0);
        return result;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
