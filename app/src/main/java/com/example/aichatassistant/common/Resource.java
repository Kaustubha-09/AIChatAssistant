package com.example.aichatassistant.common;

/**
 * Generic sealed-style result wrapper used across data and domain layers.
 * Replaces ad-hoc null-checks and exception passing through call stacks.
 */
public class Resource<T> {

    public enum Status { SUCCESS, ERROR, LOADING }

    private final Status status;
    private final T      data;
    private final String message;

    private Resource(Status status, T data, String message) {
        this.status  = status;
        this.data    = data;
        this.message = message;
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    public static <T> Resource<T> error(String message) {
        return new Resource<>(Status.ERROR, null, message);
    }

    public static <T> Resource<T> error(String message, T data) {
        return new Resource<>(Status.ERROR, data, message);
    }

    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null);
    }

    public Status getStatus()  { return status; }
    public T      getData()    { return data; }
    public String getMessage() { return message; }

    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isError()   { return status == Status.ERROR; }
    public boolean isLoading() { return status == Status.LOADING; }
}
