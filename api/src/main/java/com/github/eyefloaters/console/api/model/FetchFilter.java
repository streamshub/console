package com.github.eyefloaters.console.api.model;

import java.util.List;

public class FetchFilter {

    final String rawFilter;
    final String operator;
    final List<String> operands;

    public static FetchFilter valueOf(String filter) {
        if (filter == null) {
            return null;
        }

        int operatorIdx = filter.indexOf(',');
        String operator;
        List<String> operands;

        if (operatorIdx > -1) {
            operator = filter.substring(0, operatorIdx);
            if (operator.isBlank()) {
                operator = "eq";
            }
            operands = List.of(filter.substring(operatorIdx + 1).split(","));
        } else {
            operator = "eq";
            operands = List.of(filter);
        }

        return new FetchFilter(filter, operator, operands);
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
