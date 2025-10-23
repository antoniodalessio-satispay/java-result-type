package com.satispay.utils.resulttype;


import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LazyResultTest {

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

        assertThat(result)
                .matches(Result::isSuccess);

    }
}