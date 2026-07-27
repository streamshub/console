/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators.components;

import com.github.streamshub.systemtests.locators.Locators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Wraps a semantically-anchored PatternFly table (found via OUIA id or accessible
 * name, never an absolute DOM path) and exposes row/cell lookups scoped to that
 * table, so a specific row is found by matching its own data rather than by
 * position in the page.
 */
public class ResourceTable {
    private final Locator root;

    private ResourceTable(Locator root) {
        this.root = root;
    }

    public static ResourceTable byOuiaId(Page page, String ouiaId) {
        return new ResourceTable(Locators.byOuiaId(page, ouiaId));
    }

    public static ResourceTable byAccessibleName(Page page, String accessibleName) {
        return new ResourceTable(page.getByRole(AriaRole.TABLE, new Page.GetByRoleOptions().setName(accessibleName)));
    }

    public Locator root() {
        return root;
    }

    public Locator rows() {
        return root.locator("tbody tr");
    }

    public int rowCount() {
        return rows().count();
    }

    /** The row whose visible text contains {@code text} (e.g. a topic/user/group name the test itself created). */
    public Locator row(String text) {
        return rows().filter(new Locator.FilterOptions().setHasText(text));
    }

    /** The cell in {@code row} for the column labelled {@code dataLabel} (PatternFly's {@code Td dataLabel} prop renders a {@code data-label} attribute). */
    public Locator cell(Locator row, String dataLabel) {
        return row.locator("td[data-label='" + dataLabel + "']");
    }
}
