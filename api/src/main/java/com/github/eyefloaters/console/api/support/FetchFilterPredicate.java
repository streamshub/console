package com.github.eyefloaters.console.api.support;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.github.eyefloaters.console.api.model.FetchFilter;

public class FetchFilterPredicate<B, F> implements Predicate<B> {

    private final String operator;
    private final List<F> operands;
    private final Function<B, F> fieldSource;
    private final Pattern likePattern;

    public FetchFilterPredicate(FetchFilter filter, Function<String, F> operandParser, Function<B, F> fieldSource) {
        this.operator = filter.getOperator();
        this.operands = filter.getOperands().stream().map(operandParser).toList();
        this.fieldSource = fieldSource;

        if (operator.equals("like")) {
            // throws ClassCastException if this class is constructed with an incorrect operandParser (API bug)
            String firstOperand = (String) firstOperand();
            likePattern = Pattern.compile(firstOperand
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", "."));
        } else {
            likePattern = null;
        }
    }

    @SuppressWarnings("unchecked")
    public FetchFilterPredicate(FetchFilter filter, Function<B, F> fieldSource) {
        this(filter, op -> (F) op, fieldSource);
    }

    private F firstOperand() {
        return operands.get(0);
    }

    @Override
    public boolean test(B bean) {
        F field = fieldSource.apply(bean);

        switch (operator) {
            case "in":
                return operands.contains(field);

            case "gte": {
                @SuppressWarnings("unchecked")
                // throws ClassCastException if this class is constructed with an incorrect operandParser (API bug)
                Comparable<F> firstOperand = (Comparable<F>) firstOperand();
                return firstOperand.compareTo(field) <= 0;
            }

            case "like": {
                // throws ClassCastException if this class is constructed with an incorrect fieldSource (API bug)
                return likePattern.matcher((String) field).matches();
            }

            case "eq":
                return firstOperand().equals(field);

            default:
                /*
                 * Exclude the record. This case should never be executed if proper input
                 * validation is present for each filter parameter.
                 */
                return false;
        }
    }
}
