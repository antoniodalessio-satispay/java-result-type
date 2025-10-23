package com.satispay.utils.resulttype;

import java.util.function.Supplier;

public class Result <T, E>{
    private T data;
    private E error;

    private Result(T data, E error) {
        this.data = data;
        this.error = error;
    }

    public static <T, E> Result<T, E> success(T data) {
        return new Result<>(data, null);
    }

    public static <T, E> Result<T, E> failure(E error) {
        return new Result<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }
    public T getData() {
        return data;
    }
    public E getError() {
        return error;
    }
}
