package com.hnu.legal_cases.util;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class JSONReturnBean<T> {
    public final static int FAILED_CODE = 0;
    public final static int SUCCESS_CODE = 200;

    private Integer code=SUCCESS_CODE;
    private String message=null;
    private T data;

    public static <T> JSONReturnBean<T> success(T data) {
        return new JSONReturnBean<T>().setData(data).setMessage("success");
    }

    public static <T> JSONReturnBean<T> success(String msg) {
        return new JSONReturnBean<T>().setMessage(msg);
    }

    public static <T> JSONReturnBean<T> success(T data, String msg) {
        return new JSONReturnBean<T>().setData(data).setMessage(msg);
    }

    public static <T> JSONReturnBean<T> failed(String msg) {
        return new JSONReturnBean<T>().setCode(FAILED_CODE).setMessage(msg);
    }

    public static <T> JSONReturnBean<T> failed(Integer code, String msg) {
        return new JSONReturnBean<T>().setCode(code).setMessage(msg);
    }
}
