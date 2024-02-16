package com.github.eyefloaters.console.api.support;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.github.eyefloaters.console.api.model.FetchFilter;

public class FetchFilterPredicate<B, F> implements Predicate<B> {

    private final String name;
    private final String operator;
    private final List<F> operands;
    private final Function<B, F> fieldSource;
    private final Pattern likePattern;

    public FetchFilterPredicate(String name, FetchFilter filter, Function<String, F> operandParser, Function<B, F> fieldSource) {
        this.name = name;
        this.operator = filter.getOperator();
        this.operands = filter.getOperands().stream().map(operandParser).toList();
        this.fieldSource = fieldSource;

        if (operator.equals("like")) {
            // throws ClassCastException if this class is constructed with an incorrect operandParser (API bug)
            String firstOperand = (String) firstOperand();
            StringBuilder pattern = new StringBuilder();
            StringBuilder quoted = new StringBuilder();
            Runnable appendQuoted = () -> {
                if (quoted.length() > 0) {
                    pattern.append(Pattern.quote(quoted.toString()));
                    quoted.setLength(0);
                }
            };

            firstOperand.chars().forEach(c -> {
                switch (c) {
                    case '.':
                        appendQuoted.run();
                        pattern.append("\\.");
                        break;
                    case '*':
                        appendQuoted.run();
                        pattern.append(".*");
                        break;
                    case '?':
                        appendQuoted.run();
                        pattern.append(".");
                        break;
                    default:
                        quoted.append((char) c);
                        break;
                }
            });

            appendQuoted.run();
            likePattern = Pattern.compile(pattern.toString());
        } else {
            likePattern = null;
        }
    }

    public FetchFilterPredicate(FetchFilter filter, Function<String, F> operandParser, Function<B, F> fieldSource) {
        this(null, filter, operandParser, fieldSource);
    }

    @SuppressWarnings("unchecked")
    public FetchFilterPredicate(String name, FetchFilter filter, Function<B, F> fieldSource) {
        this(name, filter, op -> (F) op, fieldSource);
    }

    @SuppressWarnings("unchecked")
    public FetchFilterPredicate(FetchFilter filter, Function<B, F> fieldSource) {
        this(null, filter, op -> (F) op, fieldSource);
    }

    private F firstOperand() {
        return operands.get(0);
    }

    public String name() {
        return name;
    }

    public String operator() {
        return operator;
    }

    public List<F> operands() {
        return operands;
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
