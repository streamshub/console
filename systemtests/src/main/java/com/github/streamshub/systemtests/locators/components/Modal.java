/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators.components;

import com.github.streamshub.systemtests.locators.Locators;
import com.github.streamshub.systemtests.locators.TextConstants;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** Scopes locators to the currently-open PatternFly modal dialog (role=dialog). */
public final class Modal {
    private Modal() {}

    public static Locator root(Page page) {
        return page.getByRole(AriaRole.DIALOG);
    }

    public static Locator heading(Page page) {
        return root(page).getByRole(AriaRole.HEADING);
    }

    public static Locator confirmButton(Page page) {
        return Locators.buttonNamed(root(page), TextConstants.CONFIRM);
    }

    public static Locator cancelButton(Page page) {
        return Locators.buttonNamed(root(page), TextConstants.CANCEL);
    }
}
