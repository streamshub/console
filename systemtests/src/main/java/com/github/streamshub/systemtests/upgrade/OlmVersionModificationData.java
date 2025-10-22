package com.github.streamshub.systemtests.upgrade;

public class OlmVersionModificationData {
    private String oldOlmChannel;
    private String newOlmChannel;
    private String oldOperatorVersion;
    private String newOperatorVersion;

    public String getOldOlmChannel() {
        return oldOlmChannel;
    }

    public void setOldOlmChannel(String oldOlmChannel) {
        this.oldOlmChannel = oldOlmChannel;
    }

    public String getNewOlmChannel() {
        return newOlmChannel;
    }

    public void setNewOlmChannel(String newOlmChannel) {
        this.newOlmChannel = newOlmChannel;
    }

    public String getOldOperatorVersion() {
        return oldOperatorVersion;
    }

    public void setOldOperatorVersion(String oldOperatorVersion) {
        this.oldOperatorVersion = oldOperatorVersion;
    }

    public String getNewOperatorVersion() {
        return newOperatorVersion;
    }

    public void setNewOperatorVersion(String newOperatorVersion) {
        this.newOperatorVersion = newOperatorVersion;
    }
}
