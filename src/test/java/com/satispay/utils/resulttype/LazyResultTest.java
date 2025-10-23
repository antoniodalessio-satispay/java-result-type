package com.satispay.utils.resulttype;


import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
}