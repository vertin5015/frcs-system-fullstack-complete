package com.hnu.legal_cases.exception;

import lombok.Getter;

/**
 * @author baixu
 * @date 2025/8/20
 * server层异常类
 */
@Getter
public class ServiceException extends RuntimeException {

    /**
     * service层业务异常消息
     */
    private String serviceMsg = "";

    public ServiceException() {
        super();
    }

    public ServiceException(String serviceMsg) {
        super(serviceMsg, null);
        this.serviceMsg = serviceMsg;
    }

    public ServiceException(String serviceMsg, String exceptionMsg) {
        super(exceptionMsg, null);
        this.serviceMsg = serviceMsg;
    }

    public ServiceException(String serviceMsg, String exceptionMsg, Throwable cause) {
        super(exceptionMsg, cause);
        this.serviceMsg = serviceMsg;
    }
}