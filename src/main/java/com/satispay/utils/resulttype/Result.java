package com.satispay.utils.resulttype;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A type that represents the result of an operation that can either succeed with a value
 * or fail with an error. This provides a type-safe alternative to exception-based error handling.
 *
 * <p>A Result is immutable and can be in one of two states:
 * <ul>
 *   <li>Success - contains a non-null value of type T</li>
 *   <li>Failure - contains a non-null error of type E</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * Result<Integer, String> result = Result.success(42);
 * if (result.isSuccess()) {
 *     System.out.println("Value: " + result.getData());
 * }
 *
 * Result<Integer, String> error = Result.failure("Something went wrong");
 * String message = error.orElse(0);
 * }</pre>
 *
 * @param <T> the type of the success value
 * @param <E> the type of the error value
 * @see LazyResult
 */
public class Result<T, E> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final T data;
    private final E error;

    private Result(T data, E error) {
        this.data = data;
        this.error = error;
    }

    /**
     * Creates a successful Result containing the given value.
     *
     * @param <T>  the type of the success value
     * @param <E>  the type of the error value
     * @param data the success value, must not be null
     * @return a successful Result containing the given value
     * @throws NullPointerException if data is null
     */
    public static <T, E> Result<T, E> success(T data) {
        Objects.requireNonNull(data, "success data cannot be null");
        return new Result<>(data, null);
    }

    /**
     * Creates a failed Result containing the given error.
     *
     * @param <T>   the type of the success value
     * @param <E>   the type of the error value
     * @param error the error value, must not be null
     * @return a failed Result containing the given error
     * @throws NullPointerException if error is null
     */
    public static <T, E> Result<T, E> failure(E error) {
        Objects.requireNonNull(error, "error cannot be null");
        return new Result<>(null, error);
    }

    /**
     * Checks if this Result represents a success.
     *
     * @return true if this is a success, false if this is a failure
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * Returns the success value.
     * Returns null if this is a failure.
     *
     * @return the success value, or null if this is a failure
     */
    public T getData() {
        return data;
    }

    /**
     * Returns the error value.
     * Returns null if this is a success.
     *
     * @return the error value, or null if this is a success
     */
    public E getError() {
        return error;
    }

    // Functional methods

    /**
     * Converts the success value to an Optional.
     * Returns empty Optional if this is a failure.
     *
     * @return an Optional containing the success value, or empty if this is a failure
     */
    public Optional<T> toOptional() {
        return Optional.ofNullable(data);
    }

    /**
     * Transforms the success value using the provided mapper function.
     * If this is a failure, returns a failure with the same error.
     *
     * <p>Example:
     * <pre>{@code
     * Result<Integer, String> result = Result.success(5);
     * Result<String, String> mapped = result.map(i -> "Value: " + i);
     * // mapped contains "Value: 5"
     * }</pre>
     *
     * @param <U>    the type of the transformed value
     * @param mapper the function to transform the success value
     * @return a Result containing the transformed value, or the same failure
     * @throws NullPointerException if mapper is null
     */
    public <U> Result<U, E> map(Function<T, U> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return isSuccess() ? Result.success(mapper.apply(data)) : Result.failure(error);
    }

    /**
     * Transforms the success value using a function that returns a Result.
     * Useful for chaining operations that may fail.
     *
     * <p>Example:
     * <pre>{@code
     * Result<Integer, String> result = Result.success(5);
     * Result<Integer, String> doubled = result.flatMap(i ->
     *     i > 10 ? Result.failure("Too large") : Result.success(i * 2)
     * );
     * }</pre>
     *
     * @param <U>    the type of the value in the returned Result
     * @param mapper the function that returns a Result
     * @return the Result returned by the mapper, or the same failure
     * @throws NullPointerException if mapper is null
     */
    public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return isSuccess() ? mapper.apply(data) : Result.failure(error);
    }

    /**
     * Transforms the error value using the provided mapper function.
     * If this is a success, returns a success with the same data.
     *
     * <p>Example:
     * <pre>{@code
     * Result<Integer, String> result = Result.failure("error");
     * Result<Integer, Integer> mapped = result.mapError(String::length);
     * // mapped contains error value 5
     * }</pre>
     *
     * @param <E2>   the type of the transformed error
     * @param mapper the function to transform the error value
     * @return a Result with the transformed error, or the same success
     * @throws NullPointerException if mapper is null
     */
    public <E2> Result<T, E2> mapError(Function<E, E2> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return isSuccess() ? Result.success(data) : Result.failure(mapper.apply(error));
    }

    /**
     * Returns the success value or the provided default value if this is a failure.
     *
     * @param defaultValue the value to return if this is a failure
     * @return the success value, or the default value if this is a failure
     */
    public T orElse(T defaultValue) {
        return isSuccess() ? data : defaultValue;
    }

    /**
     * Returns the success value or throws an exception created by the provided function.
     *
     * <p>Example:
     * <pre>{@code
     * Integer value = result.orElseThrow(error -> new IllegalStateException(error));
     * }</pre>
     *
     * @param exceptionMapper the function that creates an exception from the error
     * @return the success value
     * @throws RuntimeException      the exception created by the mapper if this is a failure
     * @throws NullPointerException if exceptionMapper is null
     */
    public T orElseThrow(Function<E, ? extends RuntimeException> exceptionMapper) {
        Objects.requireNonNull(exceptionMapper, "exceptionMapper cannot be null");
        if (isSuccess()) {
            return data;
        }
        throw exceptionMapper.apply(error);
    }

    /**
     * Executes the provided consumer if this is a success.
     * Returns this Result for chaining.
     *
     * <p>Example:
     * <pre>{@code
     * result.ifSuccess(value -> System.out.println("Success: " + value))
     *       .ifFailure(error -> System.err.println("Error: " + error));
     * }</pre>
     *
     * @param consumer the action to execute on the success value
     * @return this Result for chaining
     * @throws NullPointerException if consumer is null
     */
    public Result<T, E> ifSuccess(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        if (isSuccess()) {
            consumer.accept(data);
        }
        return this;
    }

    /**
     * Executes the provided consumer if this is a failure.
     * Returns this Result for chaining.
     *
     * @param consumer the action to execute on the error value
     * @return this Result for chaining
     * @throws NullPointerException if consumer is null
     */
    public Result<T, E> ifFailure(Consumer<E> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        if (!isSuccess()) {
            consumer.accept(error);
        }
        return this;
    }

    /**
     * Recovers from a failure by applying the recovery function to the error.
     * If this is a success, returns this Result unchanged.
     *
     * <p>Example:
     * <pre>{@code
     * Result<Integer, String> result = Result.failure("error");
     * Result<Integer, String> recovered = result.recover(error -> 0);
     * // recovered is Result.success(0)
     * }</pre>
     *
     * @param recovery the function to convert the error to a success value
     * @return a successful Result with the recovered value, or this Result if already successful
     * @throws NullPointerException if recovery is null
     */
    public Result<T, E> recover(Function<E, T> recovery) {
        Objects.requireNonNull(recovery, "recovery cannot be null");
        return isSuccess() ? this : Result.success(recovery.apply(error));
    }

    /**
     * Combines two Results using the provided combiner function.
     * Returns a failure if either Result is a failure.
     * If both Results are failures, returns the first failure.
     *
     * <p>Example:
     * <pre>{@code
     * Result<Integer, String> r1 = Result.success(5);
     * Result<Integer, String> r2 = Result.success(10);
     * Result<Integer, String> combined = Result.combine(r1, r2, (a, b) -> a + b);
     * // combined is Result.success(15)
     * }</pre>
     *
     * @param <T1>     the type of the first Result's success value
     * @param <T2>     the type of the second Result's success value
     * @param <E>      the type of the error (must be the same for both Results)
     * @param <R>      the type of the combined result
     * @param r1       the first Result
     * @param r2       the second Result
     * @param combiner the function to combine the success values
     * @return a Result containing the combined value, or a failure if either Result failed
     * @throws NullPointerException if any parameter is null
     */
    public static <T1, T2, E, R> Result<R, E> combine(
            Result<T1, E> r1,
            Result<T2, E> r2,
            BiFunction<T1, T2, R> combiner) {
        Objects.requireNonNull(r1, "r1 cannot be null");
        Objects.requireNonNull(r2, "r2 cannot be null");
        Objects.requireNonNull(combiner, "combiner cannot be null");

        if (r1.isSuccess() && r2.isSuccess()) {
            return Result.success(combiner.apply(r1.data, r2.data));
        }
        return r1.isSuccess() ? Result.failure(r2.error) : Result.failure(r1.error);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Result<?, ?> result = (Result<?, ?>) o;
        return Objects.equals(data, result.data) && Objects.equals(error, result.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, error);
    }

    @Override
    public String toString() {
        return isSuccess() ? "Success(" + data + ")" : "Failure(" + error + ")";
    }
}
