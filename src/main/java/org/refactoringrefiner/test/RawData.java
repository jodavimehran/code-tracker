package org.refactoringrefiner.test;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class RawData {
    private final String repository;
    private final String startTag;
    private final String endTag;
    private final RefactoringResult refactoringResult;

    public RawData(String repository, String startTag, String endTag, RefactoringResult refactoringResult) {
        this.repository = repository;
        this.startTag = startTag;
        this.endTag = endTag;
        this.refactoringResult = refactoringResult;
    }

    public String getRepository() {
        return repository;
    }

    public String getStartTag() {
        return startTag;
    }

    public String getEndTag() {
        return endTag;
    }

    public RefactoringResult getRefactoringResult() {
        return refactoringResult;
    }

//    public void addFileChange(String commitId, FileChangeInfo fileChangeInfo) {
//        List<FileChangeInfo> fileChangeInfoList;
//        if (fileChangeInfoMap.containsKey(commitId)) {
//            fileChangeInfoList = fileChangeInfoMap.get(commitId);
//        } else {
//            fileChangeInfoList = new ArrayList<>();
//            fileChangeInfoMap.put(commitId, fileChangeInfoList);
//        }
//        fileChangeInfoList.add(fileChangeInfo);
//    }
//
//    public List<FileChangeInfo> getFileChangeInfoForAllCommits() {
//        return fileChangeInfoMap.entrySet().stream()
//                .flatMap(entry -> entry.getValue().stream())
//                .collect(Collectors.toList());
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        RawData rawData = (RawData) o;

        return new EqualsBuilder()
                .append(repository, rawData.repository)
                .append(startTag, rawData.startTag)
                .append(endTag, rawData.endTag)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(repository)
                .append(startTag)
                .append(endTag)
                .toHashCode();
    }
}
