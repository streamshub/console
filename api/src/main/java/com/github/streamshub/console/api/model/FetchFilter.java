package com.github.streamshub.console.api.model;

import java.util.ArrayList;
import java.util.List;

import com.github.streamshub.console.api.support.FetchFilterPredicate;

public class FetchFilter {

    final String rawFilter;
    final String operator;
    final List<String> operands;

    public static FetchFilter valueOf(String filter) {
        if (filter == null) {
            return null;
        }

        String operator;
        List<String> operands;
        List<String> parts = split(filter);

        if (parts.size() == 1) {
            operator = FetchFilterPredicate.Operator.EQUAL_TO.value();
            operands = parts; // the whole input is the operand
        } else {
            operator = parts.get(0);
            if (operator.isBlank()) {
                operator = FetchFilterPredicate.Operator.EQUAL_TO.value();
            }
            operands = parts.subList(1, parts.size());
        }

        return new FetchFilter(filter, operator, operands);
    }

    static List<String> split(String filter) {
        List<String> elements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escape = false;

        for (int i = 0; i < filter.length(); i++) {
            char c = filter.charAt(i);
            if (escape) {
                current.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == ',') {
                elements.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (escape) {
            // Dangling backslash — treat it as a literal backslash
            current.append('\\');
        }
        elements.add(current.toString());

        return elements;
    }

    public static String rawFilter(FetchFilter filter) {
        return filter != null ? filter.getRawFilter() : null;
    }

    FetchFilter(String rawFilter, String operator, List<String> operands) {
        this.rawFilter = rawFilter;
        this.operator = operator;
        this.operands = operands;
    }

    public String getRawFilter() {
        return rawFilter;
    }

    public String getOperator() {
        return operator;
    }

    public List<String> getOperands() {
        return operands;
    }

    public String getFirstOperand() {
        return operands.get(0);
    }
}
