package com.github.streamshub.systemtests.upgrade;

public class YamlVersionModificationData {
    private final String oldOperatorVersion;
    private final String newOperatorVersion;
    private final String oldOperatorCrdsUrl;
    private final String newOperatorCrdsUrl;

    public YamlVersionModificationData(String oldOperatorVersion, String newOperatorVersion, String oldOperatorCrdsUrl, String newOperatorCrdsUrl) {
        this.oldOperatorVersion = oldOperatorVersion;
        this.newOperatorVersion = newOperatorVersion;
        this.oldOperatorCrdsUrl = oldOperatorCrdsUrl;
        this.newOperatorCrdsUrl = newOperatorCrdsUrl;
    }

    public String getOldOperatorVersion() {
        return oldOperatorVersion;
    }

    public String getNewOperatorVersion() {
        return newOperatorVersion;
    }

    public String getOldOperatorCrdsUrl() {
        return oldOperatorCrdsUrl;
    }

    public String getNewOperatorCrdsUrl() {
        return newOperatorCrdsUrl;
    }
}
