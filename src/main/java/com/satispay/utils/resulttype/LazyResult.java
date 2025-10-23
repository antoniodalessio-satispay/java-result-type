package com.satispay.utils.resulttype;

import java.util.function.Function;
import java.util.function.Supplier;

public class LazyResult<T, E> {
    private Supplier<T> supplier;
    private Function<Exception, E> errorMapper;

    private LazyResult(Supplier<T> supplier, Function<Exception, E> errorMapper) {
        this.supplier = supplier;
        this.errorMapper = errorMapper;
    }

    public static <T, E> LazyResult<T, E> create(Supplier<T> supplier, Function<Exception, E> errorMapper) {
        return new LazyResult<>(supplier, errorMapper);
    }

    public Result<T, E> evaluate() {
        try {
            T data = supplier.get();
            return Result.success(data);
        } catch (Exception ex) {
            E error = errorMapper.apply(ex);
            return Result.failure(error);
        }
    }

    public static <X, T, E> LazyResult<X, E> map(LazyResult<T, E> lazyResult, Function<T, X> mapper) {
        return new LazyResult<>(
                wrapMainFunction(lazyResult, mapper),
                lazyResult.errorMapper
        );
    }

    public static <X, E2, T, E> LazyResult<X, E2> map(LazyResult<T, E> lazyResult,
                                                      Function<T, X> mapper,
                                                      Function<E, E2> exceptionMapper) {
        return new LazyResult<>(
                wrapMainFunction(lazyResult, mapper),
                wrapException(lazyResult, exceptionMapper)
        );
    }

    public static <E2, T, E> LazyResult<T, E2> mapError(LazyResult<T, E> lazyResult,
                                                        Function<E, E2> exceptionMapper) {
        return new LazyResult<>(
                lazyResult.supplier,
                wrapException(lazyResult, exceptionMapper)
        );
    }

    private static <E2, T, E> Function<Exception, E2> wrapException(LazyResult<T, E> lazyResult, Function<E, E2> exceptionMapper) {
        return lazyResult.errorMapper.andThen(exceptionMapper);
    }

    private static <X, T, E> Supplier<X> wrapMainFunction(LazyResult<T, E> lazyResult, Function<T, X> mapper) {
        return () -> mapper.apply(lazyResult.supplier.get());
    }


}
