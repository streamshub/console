package com.github.streamshub.console.api.support.serdes;

import java.io.IOException;

public interface ForceCloseable {

    void forceClose() throws IOException;

}
