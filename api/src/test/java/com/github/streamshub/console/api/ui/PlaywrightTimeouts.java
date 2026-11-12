
package com.github.streamshub.console.api.ui;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

public final class PlaywrightTimeouts {

    private static final double DEFAULT_TIMEOUT_MS = 15_000;

    private PlaywrightTimeouts() {
    }

    public static void configure(BrowserContext context) {
        context.setDefaultTimeout(DEFAULT_TIMEOUT_MS);
        PlaywrightAssertions.setDefaultAssertionTimeout(DEFAULT_TIMEOUT_MS);
    }
}
