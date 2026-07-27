/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Pattern;

/** Shared helpers for common locator patterns, so page/component classes don't repeat raw Playwright API boilerplate. */
public final class Locators {
    private Locators() {}

    /** Finds an element by its {@code data-ouia-component-id} attribute, the app's stable-identity convention. */
    public static Locator byOuiaId(Page page, String ouiaId) {
        return page.locator("[data-ouia-component-id='" + ouiaId + "']");
    }

    public static Locator buttonNamed(Page page, String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name));
    }

    public static Locator buttonNamed(Page page, String name, boolean exact) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(exact));
    }

    public static Locator buttonNamed(Page page, Pattern namePattern) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(namePattern));
    }

    public static Locator buttonNamed(Locator scope, String name) {
        return scope.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name));
    }

    /** Items of whichever PatternFly menu/dropdown is currently open, portaled to the page root - not scoped to any one page. */
    public static Locator menuItems(Page page) {
        return page.getByRole(AriaRole.MENUITEM);
    }
}
