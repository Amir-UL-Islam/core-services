package com.problemfighter.pfspring.restapi.exception;

import com.problemfighter.pfspring.restapi.common.RestSpringContext;
import com.problemfighter.pfspring.restapi.rr.ResponseProcessor;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import org.hibernate.HibernateException;
import org.springframework.core.env.Environment;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

public class ExceptionProcessor {
    private final Environment environment = RestSpringContext.environment();

    public static ExceptionProcessor instance() {
        return new ExceptionProcessor();
    }

    public String eng() {
        if (this.environment == null) {
            return null;
        }
        String[] profiles = this.environment.getActiveProfiles();
        return profiles.length > 0 ? profiles[0] : null;
    }

    public String handleHibernateException(Throwable throwable) {
        return throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage();
    }

    private String exceptionMessageGenerator(Exception exception, String message) {
        if (exception instanceof MethodArgumentTypeMismatchException) {
            return ExceptionMessage.invalidRequestParams;
        } else {
            if (exception.getCause() instanceof HibernateException) {
                message = this.handleHibernateException(exception.getCause());
            }

            return message;
        }
    }

    public MessageResponse handleException(Exception exception) {
        String message = exception.getMessage();
        String code = "1511";
        message = this.exceptionMessageGenerator(exception, message);
        if (this.eng() != null && this.eng().equals("local")) {
            exception.printStackTrace();
        }

        MessageResponse messageResponse = ResponseProcessor.errorMessage(message).setCode(code);
        messageResponse.updateErrorMessageKey((String)null);
        return messageResponse;
    }
}
