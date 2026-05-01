package com.github.streamshub.console.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.sundr.builder.annotations.Buildable;

@JsonInclude(Include.NON_NULL)
@Buildable(editableEnabled = false)
public class OptionsConfig {

    boolean showLearning = true;

    public boolean isShowLearning() {
        return showLearning;
    }

    public void setShowLearning(boolean showLearning) {
        this.showLearning = showLearning;
    }
}
