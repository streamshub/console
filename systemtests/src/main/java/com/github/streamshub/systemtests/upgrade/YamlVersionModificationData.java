package com.github.streamshub.systemtests.upgrade;

public class YamlVersionModificationData {
    private String oldOperatorVersion;
    private String newOperatorVersion;
    private String oldOperatorCrdsUrl;
    private String newOperatorCrdsUrl;

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
