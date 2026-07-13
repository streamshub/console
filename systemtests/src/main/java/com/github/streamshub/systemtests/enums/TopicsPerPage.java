package com.github.streamshub.systemtests.enums;

/**
 * Represents the selectable "topics per page" options in the pagination dropdown.
 * The dropdown always contains the options 5, 10, 20, 50, 100 (in this order from the top),
 * so each value carries its fixed 1-based position in that dropdown regardless of which
 * subset of values is actually exercised by a test.
 */
public enum TopicsPerPage {

    FIVE(5, 1),
    TEN(10, 2),
    TWENTY(20, 3),
    FIFTY(50, 4),
    HUNDRED(100, 5);

    private final int value;
    private final int dropdownPosition;

    TopicsPerPage(int value, int dropdownPosition) {
        this.value = value;
        this.dropdownPosition = dropdownPosition;
    }

    public int getValue() {
        return value;
    }

    public int getDropdownPosition() {
        return dropdownPosition;
    }
}
