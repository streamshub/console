package io.streamshub.console.api.support;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import io.streamshub.console.api.model.FetchFilter;

public class FetchFilterPredicate<B, F> implements Predicate<B> {

    public enum Operator {
        IN_LIST("in"),
        GREATER_THAN_OR_EQUAL_TO("gte"),
        LIKE_PATTERN("like", false),
        CASE_INSENSITIVE_LIKE_PATTERN("ilike", true),
        EQUAL_TO("eq"),
        CONTAINS("contains");

        private final String value;
        private final boolean glob;
        private final boolean ignoreCase;

        private Operator(String value, boolean ignoreCase) {
            this.value = value;
            this.glob = true;
            this.ignoreCase = ignoreCase;
        }

        private Operator(String value) {
            this.value = value;
            this.glob = false;
            this.ignoreCase = false;
        }

        public String value() {
            return value;
        }

        public boolean isGlob() {
            return glob;
        }

        public boolean ignoreCase() {
            return ignoreCase;
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
    private final GlobPattern likePattern;

    public FetchFilterPredicate(String name, FetchFilter filter, Function<String, F> operandParser, Function<B, Object> fieldSource) {
        this.name = name;
        this.operator = Operator.fromValue(filter.getOperator());
        this.operands = filter.getOperands().stream().map(operandParser).toList();
        this.fieldSource = fieldSource;

        if (operator.isGlob()) {
            // throws ClassCastException if this class is constructed with an incorrect operandParser (API bug)
            String firstOperand = (String) firstOperand();
            likePattern = GlobPattern.compile(firstOperand, operator.ignoreCase());
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

            case CASE_INSENSITIVE_LIKE_PATTERN, LIKE_PATTERN: {
                if (field == null) {
                    return false;
                }
                // throws ClassCastException if this class is constructed with an incorrect fieldSource (API bug)
                return likePattern.matches((String) field);
            }

            case EQUAL_TO:
                return firstOperand().equals(field);

            case CONTAINS:
                if (field instanceof Collection<?> values) {
                    // All of the requested operands must be present
                    return operands.stream().allMatch(values::contains);
                }

                return false;

            default:
                /*
                 * Exclude the record. This case should never be executed if proper input
                 * validation is present for each filter parameter.
                 */
                return false;
        }
    }
}
