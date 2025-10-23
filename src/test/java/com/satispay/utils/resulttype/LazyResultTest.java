package com.satispay.utils.resulttype;


import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class LazyResultTest {

    @Test
    public void shouldCreateAndEvaluateSuccessfulResult() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 42,
                ex -> "Error: " + ex.getMessage()
        );

        Result<Integer, String> result = lazyResult.evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(42);
        assertThat(result.getError()).isNull();
    }

    @Test
    public void shouldCreateAndEvaluateFailureResult() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> {
                    throw new RuntimeException("Something went wrong");
                },
                ex -> "Error: " + ex.getMessage()
        );

        Result<Integer, String> result = lazyResult.evaluate();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getData()).isNull();
        assertThat(result.getError()).isEqualTo("Error: Something went wrong");
    }

    @Test
    public void shouldMapSuccessFunction() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 5,
                ex -> "Error"
        );

        LazyResult<String, String> mappedLazyResult = LazyResult.map(
                lazyResult,
                i -> "Number: " + i
        );

        Result<String, String> result = mappedLazyResult.evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("Number: 5");
    }

    @Test
    public void shouldMapFailureResultPreservingError() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> {
                    throw new IllegalArgumentException("Invalid input");
                },
                ex -> "Error: " + ex.getMessage()
        );

        LazyResult<String, String> mappedLazyResult = LazyResult.map(
                lazyResult,
                i -> "Number: " + i
        );

        Result<String, String> result = mappedLazyResult.evaluate();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo("Error: Invalid input");
    }

    @Test
    public void shouldChainMultipleMaps() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 10,
                ex -> "Error: " + ex.getMessage()
        );

        LazyResult<Integer, String> doubled = LazyResult.map(lazyResult, i -> i * 2);
        LazyResult<Integer, String> plusTen = LazyResult.map(doubled, i -> i + 10);
        LazyResult<String, String> toString = LazyResult.map(plusTen, i -> "Result: " + i);

        Result<String, String> result = toString.evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("Result: 30");
    }

    @Test
    public void shouldMapWithErrorTransformation() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 5,
                ex -> "Original error"
        );

        LazyResult<String, Integer> mappedLazyResult = LazyResult.map(
                lazyResult,
                i -> "Number: " + i,
                error -> error.length()
        );

        Result<String, Integer> result = mappedLazyResult.evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("Number: 5");
    }

    @Test
    public void shouldMapErrorOnFailure() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> {
                    throw new RuntimeException("Original error");
                },
                ex -> "Error: " + ex.getMessage()
        );

        LazyResult<String, Integer> mappedLazyResult = LazyResult.map(
                lazyResult,
                i -> "Number: " + i,
                error -> error.length()
        );

        Result<String, Integer> result = mappedLazyResult.evaluate();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo(21); // "Error: Original error".length()
    }

    @Test
    public void shouldMapErrorWithoutMappingValue() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 42,
                ex -> "Error: " + ex.getMessage()
        );

        LazyResult<Integer, Integer> mappedError = LazyResult.mapError(
                lazyResult,
                error -> error.length()
        );

        Result<Integer, Integer> result = mappedError.evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(42);
    }

    @Test
    public void shouldMapErrorOnFailureCase() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> {
                    throw new RuntimeException("Failed operation");
                },
                ex -> "Error: " + ex.getMessage()
        );

        LazyResult<Integer, Integer> mappedError = LazyResult.mapError(
                lazyResult,
                error -> error.length()
        );

        Result<Integer, Integer> result = mappedError.evaluate();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo(23); // "Error: Failed operation".length()
    }

    @Test
    public void shouldChainMapAndMapError() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> {
                    throw new IllegalStateException("Invalid state");
                },
                ex -> ex.getMessage()
        );

        LazyResult<String, String> mapped = LazyResult.map(lazyResult, i -> "Value: " + i);
        LazyResult<String, String> errorMapped = LazyResult.mapError(mapped, error -> "Wrapped: " + error);

        Result<String, String> result = errorMapped.evaluate();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo("Wrapped: Invalid state");
    }

    @Test
    public void shouldHandleNullPointerException() {
        LazyResult<String, String> lazyResult = LazyResult.create(
                () -> {
                    String nullString = null;
                    return nullString.toUpperCase();
                },
                ex -> "NPE: " + ex.getClass().getSimpleName()
        );

        Result<String, String> result = lazyResult.evaluate();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo("NPE: NullPointerException");
    }

    @Test
    public void shouldLazilyEvaluateSupplier() {
        final int[] counter = {0};

        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> {
                    counter[0]++;
                    return 100;
                },
                ex -> "Error"
        );

        // Before evaluation, counter should be 0
        assertThat(counter[0]).isEqualTo(0);

        // After first evaluation, counter should be 1
        lazyResult.evaluate();
        assertThat(counter[0]).isEqualTo(1);

        // After second evaluation, counter should be 2 (not cached)
        lazyResult.evaluate();
        assertThat(counter[0]).isEqualTo(2);
    }

    @Test
    public void shouldHandleComplexPipeline() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 5,
                ex -> "Initial error: " + ex.getMessage()
        );

        LazyResult<Integer, String> pipeline = LazyResult.map(
                LazyResult.map(
                        LazyResult.map(lazyResult, i -> i * 2),
                        i -> i + 3
                ),
                i -> i * i
        );

        Result<Integer, String> result = pipeline.evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(169); // ((5 * 2) + 3)^2 = 13^2 = 169
    }

    // Tests for new instance methods

    @Test
    public void shouldUseInstanceMapMethod() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 10,
                ex -> "Error: " + ex.getMessage()
        );

        LazyResult<String, String> mapped = lazyResult.map(i -> "Value: " + i);
        Result<String, String> result = mapped.evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("Value: 10");
    }

    @Test
    public void shouldChainInstanceMapMethods() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 5,
                ex -> "Error"
        );

        Result<String, String> result = lazyResult
                .map(i -> i * 2)
                .map(i -> i + 3)
                .map(i -> "Result: " + i)
                .evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("Result: 13");
    }

    @Test
    public void shouldFlatMapLazyResults() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 5,
                ex -> "Error: " + ex.getMessage()
        );

        LazyResult<String, String> flatMapped = lazyResult.flatMap(i ->
                LazyResult.create(
                        () -> "Value: " + (i * 2),
                        ex -> "Inner error: " + ex.getMessage()
                )
        );

        Result<String, String> result = flatMapped.evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("Value: 10");
    }

    @Test
    public void shouldHandleFlatMapFailureInFirstOperation() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> {
                    throw new RuntimeException("First failed");
                },
                ex -> "Error: " + ex.getMessage()
        );

        LazyResult<String, String> flatMapped = lazyResult.flatMap(i ->
                LazyResult.create(
                        () -> "Value: " + i,
                        ex -> "Inner error"
                )
        );

        Result<String, String> result = flatMapped.evaluate();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo("Error: First failed");
    }

    @Test
    public void shouldHandleFlatMapFailureInSecondOperation() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 5,
                ex -> "Error: " + ex.getMessage()
        );

        LazyResult<String, String> flatMapped = lazyResult.flatMap(i ->
                LazyResult.create(
                        () -> {
                            throw new RuntimeException("Second failed");
                        },
                        ex -> "Inner error: " + ex.getMessage()
                )
        );

        Result<String, String> result = flatMapped.evaluate();

        assertThat(result.isSuccess()).isFalse();
        // Note: flatMap uses the outer error mapper when the inner operation fails
        assertThat(result.getError()).isEqualTo("Error: Second failed");
    }

    @Test
    public void shouldPeekAtValue() {
        final int[] peekedValue = {0};

        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 42,
                ex -> "Error"
        );

        Result<Integer, String> result = lazyResult
                .peek(value -> peekedValue[0] = value)
                .evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(42);
        assertThat(peekedValue[0]).isEqualTo(42);
    }

    @Test
    public void shouldNotPeekOnFailure() {
        final boolean[] peeked = {false};

        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> {
                    throw new RuntimeException("Failed");
                },
                ex -> "Error"
        );

        Result<Integer, String> result = lazyResult
                .peek(value -> peeked[0] = true)
                .evaluate();

        assertThat(result.isSuccess()).isFalse();
        assertThat(peeked[0]).isFalse();
    }

    @Test
    public void shouldRecoverFromError() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> {
                    throw new RuntimeException("Failed");
                },
                ex -> "Error: " + ex.getMessage()
        );

        Result<Integer, String> result = lazyResult
                .recover(error -> 0)
                .evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(0);
    }

    @Test
    public void shouldNotRecoverFromSuccess() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 42,
                ex -> "Error"
        );

        Result<Integer, String> result = lazyResult
                .recover(error -> 0)
                .evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(42);
    }

    @Test
    public void shouldRecoverWithErrorInformation() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> {
                    throw new RuntimeException("Connection failed");
                },
                ex -> ex.getMessage()
        );

        Result<Integer, String> result = lazyResult
                .recover(error -> error.length())
                .evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(17); // "Connection failed".length()
    }

    @Test
    public void shouldThrowExceptionForNullSupplier() {
        assertThatThrownBy(() -> LazyResult.create(null, ex -> "Error"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("supplier cannot be null");
    }

    @Test
    public void shouldThrowExceptionForNullErrorMapper() {
        assertThatThrownBy(() -> LazyResult.create(() -> 42, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("errorMapper cannot be null");
    }

    @Test
    public void shouldThrowExceptionForNullMapFunction() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(() -> 42, ex -> "Error");

        assertThatThrownBy(() -> lazyResult.map(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("mapper cannot be null");
    }

    @Test
    public void shouldCombineMultipleOperations() {
        LazyResult<Integer, String> lazyResult = LazyResult.create(
                () -> 10,
                ex -> "Error: " + ex.getMessage()
        );

        final int[] peekedValue = {0};

        Result<String, String> result = lazyResult
                .map(i -> i * 2)                    // 20
                .peek(i -> peekedValue[0] = i)      // peek at 20
                .map(i -> i + 5)                    // 25
                .flatMap(i -> LazyResult.create(
                        () -> i * 2,                // 50
                        ex -> "Flat error"
                ))
                .map(i -> "Final: " + i)
                .evaluate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("Final: 50");
        assertThat(peekedValue[0]).isEqualTo(20);
    }
}