package com.github.streamshub.console.api.support;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.github.streamshub.console.api.model.FetchFilter;

public class FetchFilterPredicate<B, F> implements Predicate<B> {

    public enum Operator {
        IN_LIST("in"),
        GREATER_THAN_OR_EQUAL_TO("gte"),
        LIKE_PATTERN("like"),
        EQUAL_TO("eq");

        private final String value;

        private Operator(String value) {
            this.value = value;
        }

        public static Operator fromValue(String value) {
            Objects.requireNonNull(value);

            for (Operator o : values()) {
                if (o.value.equals(value)) {
                    return o;
                }
            }

            throw new IllegalArgumentException(value);
        }
    }

    private final String name;
    private final Operator operator;
    private final List<F> operands;
    private final Function<B, Object> fieldSource;
    private final Pattern likePattern;

    public FetchFilterPredicate(String name, FetchFilter filter, Function<String, F> operandParser, Function<B, Object> fieldSource) {
        this.name = name;
        this.operator = Operator.fromValue(filter.getOperator());
        this.operands = filter.getOperands().stream().map(operandParser).toList();
        this.fieldSource = fieldSource;

        if (operator == Operator.LIKE_PATTERN) {
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

    public FetchFilterPredicate(FetchFilter filter, Function<String, F> operandParser, Function<B, Object> fieldSource) {
        this(null, filter, operandParser, fieldSource);
    }

    @SuppressWarnings("unchecked")
    public FetchFilterPredicate(String name, FetchFilter filter, Function<B, Object> fieldSource) {
        this(name, filter, op -> (F) op, fieldSource);
    }

    @SuppressWarnings("unchecked")
    public FetchFilterPredicate(FetchFilter filter, Function<B, Object> fieldSource) {
        this(null, filter, op -> (F) op, fieldSource);
    }

    private F firstOperand() {
        return operands.get(0);
    }

    public String name() {
        return name;
    }

    public Operator operator() {
        return operator;
    }

    public List<F> operands() {
        return operands;
    }

    @Override
    public boolean test(B bean) {
        Object field = fieldSource.apply(bean);

        switch (operator) {
            case IN_LIST: {
                if (field instanceof Collection<?> values) {
                    /*
                     * Here we treat any intersection of the bean collection property
                     * and the operands as true.
                     *
                     *  E.g. if the bean has values of [ "string1", "string2" ] and the
                     *  operands in the request have [ "string2", "string3" ], the result
                     *  will be true.
                     */
                    return operands.stream().anyMatch(values::contains);
                }

                return operands.contains(field);
            }

            case GREATER_THAN_OR_EQUAL_TO: {
                @SuppressWarnings("unchecked")
                // throws ClassCastException if this class is constructed with an incorrect operandParser (API bug)
                Comparable<F> firstOperand = (Comparable<F>) firstOperand();
                @SuppressWarnings("unchecked")
                // throws ClassCastException if this class is constructed for an incompatible bean property (API bug)
                F comparableField = (F) field;
                return firstOperand.compareTo(comparableField) <= 0;
            }

            case LIKE_PATTERN: {
                // throws ClassCastException if this class is constructed with an incorrect fieldSource (API bug)
                return likePattern.matcher((String) field).matches();
            }

            case EQUAL_TO:
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
