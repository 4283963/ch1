package com.smartpark.dto;

public class Response<T> {
    private Integer code;
    private String message;
    private T data;

    public Response() {
    }

    public Response(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Response<T> success(T data) {
        Response<T> resp = new Response<>();
        resp.setCode(200);
        resp.setMessage("success");
        resp.setData(data);
        return resp;
    }

    public static <T> Response<T> success() {
        Response<T> resp = new Response<>();
        resp.setCode(200);
        resp.setMessage("success");
        resp.setData(null);
        return resp;
    }

    public static <T> Response<T> fail(String message) {
        Response<T> resp = new Response<>();
        resp.setCode(500);
        resp.setMessage(message);
        resp.setData(null);
        return resp;
    }

    public static <T> Response<T> fail(Integer code, String message) {
        Response<T> resp = new Response<>();
        resp.setCode(code);
        resp.setMessage(message);
        resp.setData(null);
        return resp;
    }

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
