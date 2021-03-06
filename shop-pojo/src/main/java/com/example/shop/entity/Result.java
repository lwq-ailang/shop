package com.example.shop.entity;

import java.io.Serializable;

/**
 * 结果实体类
 */
public class Result implements Serializable {

    private static final long serialVersionUID = 6437749289863220275L;
    private Boolean success;
    private String message;
    private int code;

    public Result() {
    }

    public Result(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public Result(Boolean success, String message, int code) {
        this.success = success;
        this.message = message;
        this.code = code;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "Result{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}
