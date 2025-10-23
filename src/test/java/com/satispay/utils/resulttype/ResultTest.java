package com.satispay.utils.resulttype;

import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class ResultTest {

    @Test
    public void shouldCreateSuccessResult() {
        Result<Integer, String> result = Result.success(42);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(42);
        assertThat(result.getError()).isNull();
    }

    @Test
    public void shouldCreateFailureResult() {
        Result<Integer, String> result = Result.failure("Error occurred");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getData()).isNull();
        assertThat(result.getError()).isEqualTo("Error occurred");
    }

    @Test
    public void shouldThrowExceptionWhenCreatingSuccessWithNull() {
        assertThatThrownBy(() -> Result.success(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("success data cannot be null");
    }

    @Test
    public void shouldThrowExceptionWhenCreatingFailureWithNull() {
        assertThatThrownBy(() -> Result.failure(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("error cannot be null");
    }

    @Test
    public void shouldConvertSuccessToOptional() {
        Result<Integer, String> result = Result.success(42);
        Optional<Integer> optional = result.toOptional();

        assertThat(optional.isPresent()).isTrue();
        assertThat(optional.get()).isEqualTo(42);
    }

    @Test
    public void shouldConvertFailureToEmptyOptional() {
        Result<Integer, String> result = Result.failure("Error");
        Optional<Integer> optional = result.toOptional();

        assertThat(optional.isPresent()).isFalse();
    }

    @Test
    public void shouldMapSuccessValue() {
        Result<Integer, String> result = Result.success(5);
        Result<String, String> mapped = result.map(i -> "Number: " + i);

        assertThat(mapped.isSuccess()).isTrue();
        assertThat(mapped.getData()).isEqualTo("Number: 5");
    }

    @Test
    public void shouldNotMapFailureValue() {
        Result<Integer, String> result = Result.failure("Error");
        Result<String, String> mapped = result.map(i -> "Number: " + i);

        assertThat(mapped.isSuccess()).isFalse();
        assertThat(mapped.getError()).isEqualTo("Error");
    }

    @Test
    public void shouldFlatMapSuccessValue() {
        Result<Integer, String> result = Result.success(5);
        Result<String, String> flatMapped = result.flatMap(i -> Result.success("Value: " + i));

        assertThat(flatMapped.isSuccess()).isTrue();
        assertThat(flatMapped.getData()).isEqualTo("Value: 5");
    }

    @Test
    public void shouldFlatMapToFailure() {
        Result<Integer, String> result = Result.success(5);
        Result<String, String> flatMapped = result.flatMap(i -> Result.failure("Failed"));

        assertThat(flatMapped.isSuccess()).isFalse();
        assertThat(flatMapped.getError()).isEqualTo("Failed");
    }

    @Test
    public void shouldNotFlatMapFailure() {
        Result<Integer, String> result = Result.failure("Original error");
        Result<String, String> flatMapped = result.flatMap(i -> Result.success("Value: " + i));

        assertThat(flatMapped.isSuccess()).isFalse();
        assertThat(flatMapped.getError()).isEqualTo("Original error");
    }

    @Test
    public void shouldMapErrorValue() {
        Result<Integer, String> result = Result.failure("Error");
        Result<Integer, Integer> mapped = result.mapError(String::length);

        assertThat(mapped.isSuccess()).isFalse();
        assertThat(mapped.getError()).isEqualTo(5);
    }

    @Test
    public void shouldNotMapSuccessError() {
        Result<Integer, String> result = Result.success(42);
        Result<Integer, Integer> mapped = result.mapError(String::length);

        assertThat(mapped.isSuccess()).isTrue();
        assertThat(mapped.getData()).isEqualTo(42);
    }

    @Test
    public void shouldReturnValueWithOrElse() {
        Result<Integer, String> result = Result.success(42);
        Integer value = result.orElse(0);

        assertThat(value).isEqualTo(42);
    }

    @Test
    public void shouldReturnDefaultValueWithOrElse() {
        Result<Integer, String> result = Result.failure("Error");
        Integer value = result.orElse(0);

        assertThat(value).isEqualTo(0);
    }

    @Test
    public void shouldReturnValueWithOrElseThrow() {
        Result<Integer, String> result = Result.success(42);
        Integer value = result.orElseThrow(error -> new RuntimeException(error));

        assertThat(value).isEqualTo(42);
    }

    @Test
    public void shouldThrowExceptionWithOrElseThrow() {
        Result<Integer, String> result = Result.failure("Error message");

        assertThatThrownBy(() -> result.orElseThrow(RuntimeException::new))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error message");
    }

    @Test
    public void shouldExecuteIfSuccess() {
        Result<Integer, String> result = Result.success(42);
        AtomicBoolean executed = new AtomicBoolean(false);

        result.ifSuccess(value -> executed.set(true));

        assertThat(executed.get()).isTrue();
    }

    @Test
    public void shouldNotExecuteIfSuccessOnFailure() {
        Result<Integer, String> result = Result.failure("Error");
        AtomicBoolean executed = new AtomicBoolean(false);

        result.ifSuccess(value -> executed.set(true));

        assertThat(executed.get()).isFalse();
    }

    @Test
    public void shouldExecuteIfFailure() {
        Result<Integer, String> result = Result.failure("Error");
        AtomicBoolean executed = new AtomicBoolean(false);

        result.ifFailure(error -> executed.set(true));

        assertThat(executed.get()).isTrue();
    }

    @Test
    public void shouldNotExecuteIfFailureOnSuccess() {
        Result<Integer, String> result = Result.success(42);
        AtomicBoolean executed = new AtomicBoolean(false);

        result.ifFailure(error -> executed.set(true));

        assertThat(executed.get()).isFalse();
    }

    @Test
    public void shouldChainIfSuccessAndIfFailure() {
        Result<Integer, String> result = Result.success(42);
        AtomicBoolean successExecuted = new AtomicBoolean(false);
        AtomicBoolean failureExecuted = new AtomicBoolean(false);

        result.ifSuccess(value -> successExecuted.set(true))
              .ifFailure(error -> failureExecuted.set(true));

        assertThat(successExecuted.get()).isTrue();
        assertThat(failureExecuted.get()).isFalse();
    }

    @Test
    public void shouldRecoverFromFailure() {
        Result<Integer, String> result = Result.failure("Error");
        Result<Integer, String> recovered = result.recover(error -> 0);

        assertThat(recovered.isSuccess()).isTrue();
        assertThat(recovered.getData()).isEqualTo(0);
    }

    @Test
    public void shouldNotRecoverFromSuccess() {
        Result<Integer, String> result = Result.success(42);
        Result<Integer, String> recovered = result.recover(error -> 0);

        assertThat(recovered.isSuccess()).isTrue();
        assertThat(recovered.getData()).isEqualTo(42);
    }

    @Test
    public void shouldCombineTwoSuccesses() {
        Result<Integer, String> r1 = Result.success(5);
        Result<Integer, String> r2 = Result.success(10);

        Result<Integer, String> combined = Result.combine(r1, r2, (a, b) -> a + b);

        assertThat(combined.isSuccess()).isTrue();
        assertThat(combined.getData()).isEqualTo(15);
    }

    @Test
    public void shouldFailCombineIfFirstFails() {
        Result<Integer, String> r1 = Result.failure("Error 1");
        Result<Integer, String> r2 = Result.success(10);

        Result<Integer, String> combined = Result.combine(r1, r2, (a, b) -> a + b);

        assertThat(combined.isSuccess()).isFalse();
        assertThat(combined.getError()).isEqualTo("Error 1");
    }

    @Test
    public void shouldFailCombineIfSecondFails() {
        Result<Integer, String> r1 = Result.success(5);
        Result<Integer, String> r2 = Result.failure("Error 2");

        Result<Integer, String> combined = Result.combine(r1, r2, (a, b) -> a + b);

        assertThat(combined.isSuccess()).isFalse();
        assertThat(combined.getError()).isEqualTo("Error 2");
    }

    @Test
    public void shouldHaveCorrectToString() {
        Result<Integer, String> success = Result.success(42);
        Result<Integer, String> failure = Result.failure("Error");

        assertThat(success.toString()).isEqualTo("Success(42)");
        assertThat(failure.toString()).isEqualTo("Failure(Error)");
    }

    @Test
    public void shouldHaveCorrectEquals() {
        Result<Integer, String> r1 = Result.success(42);
        Result<Integer, String> r2 = Result.success(42);
        Result<Integer, String> r3 = Result.success(99);
        Result<Integer, String> r4 = Result.failure("Error");

        assertThat(r1.equals(r2)).isTrue();
        assertThat(r1.equals(r3)).isFalse();
        assertThat(r1.equals(r4)).isFalse();
    }

    @Test
    public void shouldHaveCorrectHashCode() {
        Result<Integer, String> r1 = Result.success(42);
        Result<Integer, String> r2 = Result.success(42);

        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }
}
