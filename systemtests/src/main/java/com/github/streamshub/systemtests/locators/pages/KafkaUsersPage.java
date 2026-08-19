/*
 * Copyright Console Authors.
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.github.streamshub.systemtests.locators.pages;

import com.github.streamshub.systemtests.locators.IdConstants;
import com.github.streamshub.systemtests.locators.Locators;
import com.github.streamshub.systemtests.locators.OuiaIdConstants;
import com.github.streamshub.systemtests.locators.TextConstants;
import com.github.streamshub.systemtests.locators.components.ResourceTable;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public final class KafkaUsersPage {
    private KafkaUsersPage() {}

    public static ResourceTable table(Page page) {
        return ResourceTable.byOuiaId(page, OuiaIdConstants.KAFKA_USERS_LISTING);
    }

    public static Locator usernameFilterInput(Page page) {
        return page.locator(IdConstants.USERNAME_FILTER);
    }

    public static Locator userNameHeading(Page page) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1));
    }

    public static Locator nameField(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.USER_DETAIL_NAME);
    }

    public static Locator usernameField(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.USER_DETAIL_USERNAME);
    }

    public static Locator authenticationField(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.USER_DETAIL_AUTHENTICATION);
    }

    public static Locator creationTimeField(Page page) {
        return Locators.byOuiaId(page, OuiaIdConstants.USER_DETAIL_CREATION_TIME);
    }

    public static ResourceTable authorizationTable(Page page) {
        return ResourceTable.byAccessibleName(page, TextConstants.AUTHORIZATION);
    }
}
