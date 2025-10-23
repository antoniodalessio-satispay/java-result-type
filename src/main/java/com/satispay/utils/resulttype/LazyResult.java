package com.satispay.utils.resulttype;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a deferred computation that will produce a {@link Result} when evaluated.
 * LazyResult allows you to compose and chain operations before actual execution,
 * providing lazy evaluation semantics.
 *
 * <p>A LazyResult wraps a supplier that may throw an exception, and an error mapper
 * that converts exceptions to error values. The computation is not executed until
 * {@link #evaluate()} is called.
 *
 * <p>LazyResult is immutable and thread-safe. Each operation (map, flatMap, etc.)
 * returns a new LazyResult without modifying the original.
 *
 * <p>Example usage:
 * <pre>{@code
 * LazyResult<Integer, String> result = LazyResult.create(
 *     () -> performExpensiveOperation(),
 *     ex -> "Error: " + ex.getMessage()
 * );
 *
 * // Chain operations without executing
 * LazyResult<String, String> transformed = result
 *     .map(i -> i * 2)
 *     .map(i -> "Value: " + i);
 *
 * // Execute only when needed
 * Result<String, String> final = transformed.evaluate();
 * }</pre>
 *
 * @param <T> the type of the success value
 * @param <E> the type of the error value
 * @see Result
 */
public class LazyResult<T, E> {
    private final Supplier<T> supplier;
    private final Function<Exception, E> errorMapper;

    private LazyResult(Supplier<T> supplier, Function<Exception, E> errorMapper) {
        this.supplier = supplier;
        this.errorMapper = errorMapper;
    }

    /**
     * Creates a LazyResult from a supplier and an error mapper.
     * The supplier will be executed lazily when evaluate() is called.
     *
     * <p>Example:
     * <pre>{@code
     * LazyResult<String, String> result = LazyResult.create(
     *     () -> fetchDataFromDatabase(),
     *     ex -> "Database error: " + ex.getMessage()
     * );
     * }</pre>
     *
     * @param <T>         the type of the success value
     * @param <E>         the type of the error value
     * @param supplier    the computation to execute, must not be null
     * @param errorMapper the function to map exceptions to errors, must not be null
     * @return a new LazyResult wrapping the computation
     * @throws NullPointerException if supplier or errorMapper is null
     */
    public static <T, E> LazyResult<T, E> create(Supplier<T> supplier, Function<Exception, E> errorMapper) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        Objects.requireNonNull(errorMapper, "errorMapper cannot be null");
        return new LazyResult<>(supplier, errorMapper);
    }

    /**
     * Evaluates the lazy computation and returns a Result.
     * Any exception thrown by the supplier will be caught and mapped to an error.
     *
     * <p>Note: Each call to evaluate() will re-execute the computation.
     * Results are not cached.
     *
     * @return a Result containing either the computed value or an error
     */
    public Result<T, E> evaluate() {
        try {
            T data = supplier.get();
            return Result.success(data);
        } catch (Exception ex) {
            E error = errorMapper.apply(ex);
            return Result.failure(error);
        }
    }

    // Instance methods for fluent API

    /**
     * Transforms the success value using the provided mapper function.
     * The error type remains unchanged.
     *
     * <p>Example:
     * <pre>{@code
     * LazyResult<Integer, String> result = LazyResult.create(() -> 5, ex -> "Error");
     * LazyResult<String, String> mapped = result.map(i -> "Number: " + i);
     * }</pre>
     *
     * @param <X>    the type of the transformed value
     * @param mapper the function to transform the success value
     * @return a new LazyResult with the transformation applied
     * @throws NullPointerException if mapper is null
     */
    public <X> LazyResult<X, E> map(Function<T, X> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return new LazyResult<>(
                wrapMainFunction(this, mapper),
                this.errorMapper
        );
    }

    /**
     * Transforms both the success value and error type using the provided mapper functions.
     *
     * <p>Example:
     * <pre>{@code
     * LazyResult<Integer, String> result = LazyResult.create(() -> 5, ex -> ex.getMessage());
     * LazyResult<String, Integer> mapped = result.map(
     *     i -> "Value: " + i,
     *     error -> error.length()
     * );
     * }</pre>
     *
     * @param <X>              the type of the transformed value
     * @param <E2>             the type of the transformed error
     * @param mapper           the function to transform the success value
     * @param exceptionMapper  the function to transform the error value
     * @return a new LazyResult with both transformations applied
     * @throws NullPointerException if mapper or exceptionMapper is null
     */
    public <X, E2> LazyResult<X, E2> map(Function<T, X> mapper, Function<E, E2> exceptionMapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(exceptionMapper, "exceptionMapper cannot be null");
        return new LazyResult<>(
                wrapMainFunction(this, mapper),
                wrapException(this, exceptionMapper)
        );
    }

    /**
     * Transforms the error type using the provided mapper function.
     * The success value type remains unchanged.
     *
     * <p>Example:
     * <pre>{@code
     * LazyResult<Integer, String> result = LazyResult.create(() -> 5, ex -> "Error");
     * LazyResult<Integer, ErrorCode> mapped = result.mapError(
     *     error -> new ErrorCode(500, error)
     * );
     * }</pre>
     *
     * @param <E2>             the type of the transformed error
     * @param exceptionMapper  the function to transform the error value
     * @return a new LazyResult with the error transformation applied
     * @throws NullPointerException if exceptionMapper is null
     */
    public <E2> LazyResult<T, E2> mapError(Function<E, E2> exceptionMapper) {
        Objects.requireNonNull(exceptionMapper, "exceptionMapper cannot be null");
        return new LazyResult<>(
                this.supplier,
                wrapException(this, exceptionMapper)
        );
    }

    /**
     * Transforms the success value using a function that returns another LazyResult.
     * Useful for chaining lazy operations that may fail.
     *
     * <p>This is the monadic bind operation for LazyResult. It allows sequencing
     * dependent computations where the second computation depends on the result
     * of the first.
     *
     * <p>Example:
     * <pre>{@code
     * LazyResult<User, String> user = LazyResult.create(() -> findUser(id), ex -> "User not found");
     * LazyResult<Account, String> account = user.flatMap(u ->
     *     LazyResult.create(() -> findAccount(u.getId()), ex -> "Account not found")
     * );
     * }</pre>
     *
     * @param <X>    the type of the value in the returned LazyResult
     * @param mapper the function that returns a LazyResult
     * @return a new LazyResult representing the sequenced computation
     * @throws NullPointerException if mapper is null
     */
    public <X> LazyResult<X, E> flatMap(Function<T, LazyResult<X, E>> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return new LazyResult<>(
                () -> {
                    T value = this.supplier.get();
                    return mapper.apply(value).supplier.get();
                },
                this.errorMapper
        );
    }

    /**
     * Executes the provided consumer with the success value when evaluated.
     * Useful for side effects like logging without transforming the value.
     *
     * <p>The action is only executed if the computation succeeds. If an exception
     * occurs, the action is not executed.
     *
     * <p>Example:
     * <pre>{@code
     * result.peek(value -> logger.info("Value: " + value))
     *       .map(value -> value * 2)
     *       .evaluate();
     * }</pre>
     *
     * @param action the action to execute on the success value
     * @return a new LazyResult that executes the action before returning the value
     * @throws NullPointerException if action is null
     */
    public LazyResult<T, E> peek(Consumer<T> action) {
        Objects.requireNonNull(action, "action cannot be null");
        return new LazyResult<>(
                () -> {
                    T value = this.supplier.get();
                    action.accept(value);
                    return value;
                },
                this.errorMapper
        );
    }

    /**
     * Recovers from a failure by applying the recovery function to the error.
     * The recovery function is called only if an exception occurs during evaluation.
     *
     * <p>This allows providing fallback values when operations fail.
     *
     * <p>Example:
     * <pre>{@code
     * LazyResult<Config, String> config = LazyResult.create(
     *     () -> loadConfigFromFile(),
     *     ex -> "Failed to load config"
     * ).recover(error -> getDefaultConfig());
     * }</pre>
     *
     * @param recovery the function to convert the error to a success value
     * @return a new LazyResult that recovers from failures
     * @throws NullPointerException if recovery is null
     */
    public LazyResult<T, E> recover(Function<E, T> recovery) {
        Objects.requireNonNull(recovery, "recovery cannot be null");
        return new LazyResult<>(
                () -> {
                    try {
                        return this.supplier.get();
                    } catch (Exception ex) {
                        E error = this.errorMapper.apply(ex);
                        return recovery.apply(error);
                    }
                },
                this.errorMapper
        );
    }

    // Static methods (kept for backward compatibility)

    /**
     * Static version of {@link #map(Function)}.
     * Prefer using the instance method for better readability.
     *
     * @param <X>         the type of the transformed value
     * @param <T>         the type of the original value
     * @param <E>         the type of the error
     * @param lazyResult  the LazyResult to transform
     * @param mapper      the transformation function
     * @return a new LazyResult with the transformation applied
     * @throws NullPointerException if lazyResult or mapper is null
     * @deprecated Use {@link #map(Function)} instance method instead for better readability
     */
    public static <X, T, E> LazyResult<X, E> map(LazyResult<T, E> lazyResult, Function<T, X> mapper) {
        Objects.requireNonNull(lazyResult, "lazyResult cannot be null");
        return lazyResult.map(mapper);
    }

    /**
     * Static version of {@link #map(Function, Function)}.
     * Prefer using the instance method for better readability.
     *
     * @param <X>              the type of the transformed value
     * @param <E2>             the type of the transformed error
     * @param <T>              the type of the original value
     * @param <E>              the type of the original error
     * @param lazyResult       the LazyResult to transform
     * @param mapper           the function to transform the success value
     * @param exceptionMapper  the function to transform the error value
     * @return a new LazyResult with both transformations applied
     * @throws NullPointerException if any parameter is null
     * @deprecated Use {@link #map(Function, Function)} instance method instead for better readability
     */
    public static <X, E2, T, E> LazyResult<X, E2> map(LazyResult<T, E> lazyResult,
                                                      Function<T, X> mapper,
                                                      Function<E, E2> exceptionMapper) {
        Objects.requireNonNull(lazyResult, "lazyResult cannot be null");
        return lazyResult.map(mapper, exceptionMapper);
    }

    /**
     * Static version of {@link #mapError(Function)}.
     * Prefer using the instance method for better readability.
     *
     * @param <E2>             the type of the transformed error
     * @param <T>              the type of the value
     * @param <E>              the type of the original error
     * @param lazyResult       the LazyResult to transform
     * @param exceptionMapper  the function to transform the error value
     * @return a new LazyResult with the error transformation applied
     * @throws NullPointerException if lazyResult or exceptionMapper is null
     * @deprecated Use {@link #mapError(Function)} instance method instead for better readability
     */
    public static <E2, T, E> LazyResult<T, E2> mapError(LazyResult<T, E> lazyResult,
                                                        Function<E, E2> exceptionMapper) {
        Objects.requireNonNull(lazyResult, "lazyResult cannot be null");
        return lazyResult.mapError(exceptionMapper);
    }

    // Private helper methods

    /**
     * Wraps the error mapper by composing it with an additional transformation.
     */
    private static <E2, T, E> Function<Exception, E2> wrapException(LazyResult<T, E> lazyResult, Function<E, E2> exceptionMapper) {
        return lazyResult.errorMapper.andThen(exceptionMapper);
    }

    /**
     * Wraps the supplier by composing it with a transformation function.
     */
    private static <X, T, E> Supplier<X> wrapMainFunction(LazyResult<T, E> lazyResult, Function<T, X> mapper) {
        return () -> mapper.apply(lazyResult.supplier.get());
    }
}
