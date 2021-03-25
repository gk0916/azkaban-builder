package com.az.workflow.exception;

public class ServerException extends RuntimeException {

    private static final long serialVersionUID = -1719909808663638420L;

    public String code;

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


}
