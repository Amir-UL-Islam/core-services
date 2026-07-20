package com.problemfighter.pfspring.restapi.exception;

import com.problemfighter.pfspring.restapi.common.ApiRestException;
import com.problemfighter.pfspring.restapi.rr.ResponseProcessor;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Centralized exception-to-response mapping used by all REST controllers.
 */
@RestControllerAdvice
public class GlobalApiExceptionHandler {

    @ExceptionHandler(ApiRestException.class)
    public ResponseEntity<Object> handleApiRestException(final ApiRestException exception) {
        final Object error = exception.getError();
        if (error instanceof MessageResponse messageResponse) {
            return ResponseEntity.status(resolveHttpStatus(messageResponse.code)).body(messageResponse);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<MessageResponse> handleResponseStatusException(final ResponseStatusException exception) {
        final HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status).body(buildResponseForHttpStatus(status, exception.getReason()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<MessageResponse> handleValidationException(final Exception exception) {
        return ResponseEntity.badRequest().body(ResponseProcessor.validationError(resolveValidationMessage(exception)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<MessageResponse> handleHttpRequestMethodNotSupportedException(
            final HttpRequestMethodNotSupportedException exception
    ) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ResponseProcessor.badRequest(exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageResponse> handleDataIntegrityViolationException(final DataIntegrityViolationException exception) {
        final String message = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage()
                : ExceptionMessage.conflict;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseProcessor.conflict(message));
    }


    @ExceptionHandler({IllegalArgumentException.class, NoSuchElementException.class})
    public ResponseEntity<MessageResponse> handleBadRequestException(final RuntimeException exception) {
        return ResponseEntity.badRequest().body(ResponseProcessor.badRequest(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleUnhandledException(final Exception exception) {
        final HttpStatus annotatedStatus = resolveAnnotatedStatus(exception);
        if (annotatedStatus != null) {
            return ResponseEntity.status(annotatedStatus)
                    .body(buildResponseForHttpStatus(annotatedStatus, exception.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExceptionProcessor.instance().handleException(exception));
    }

    private String resolveValidationMessage(final Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException
                && methodArgumentNotValidException.getBindingResult().hasErrors()) {
            return methodArgumentNotValidException.getBindingResult().getAllErrors().stream()
                    .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getCode())
                    .collect(Collectors.joining(", "));
        }

        if (exception instanceof BindException bindException && bindException.hasErrors()) {
            return bindException.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getCode())
                    .collect(Collectors.joining(", "));
        }

        return exception.getMessage() != null ? exception.getMessage() : ExceptionMessage.validationError;
    }

    private MessageResponse buildResponseForHttpStatus(final HttpStatus status, final String message) {
        final String messageToUse = message != null && !message.isBlank() ? message : status.getReasonPhrase();

        if (status == HttpStatus.NOT_FOUND) {
            return ResponseProcessor.notFound(messageToUse);
        }
        if (status == HttpStatus.UNAUTHORIZED) {
            return ResponseProcessor.unauthorized(messageToUse);
        }
        if (status == HttpStatus.FORBIDDEN) {
            return ResponseProcessor.forbidden(messageToUse);
        }
        if (status == HttpStatus.CONFLICT) {
            return ResponseProcessor.conflict(messageToUse);
        }
        if (status == HttpStatus.UNPROCESSABLE_CONTENT) {
            return ResponseProcessor.validationError(messageToUse);
        }
        if (status.is4xxClientError()) {
            return ResponseProcessor.badRequest(messageToUse);
        }

        return ResponseProcessor.codeError(messageToUse);
    }

    private HttpStatus resolveHttpStatus(final String code) {
        if (ErrorCode.badRequest.equals(code) || ErrorCode.validationError.equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ErrorCode.unauthorized.equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ErrorCode.forbidden.equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (ErrorCode.notFound.equals(code)) {
            return HttpStatus.NOT_FOUND;
        }
        if (ErrorCode.conflict.equals(code)) {
            return HttpStatus.CONFLICT;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private HttpStatus resolveAnnotatedStatus(final Exception exception) {
        final ResponseStatus responseStatus = AnnotationUtils.findAnnotation(exception.getClass(), ResponseStatus.class);
        if (responseStatus == null) {
            return null;
        }

        if (responseStatus.code() != HttpStatus.INTERNAL_SERVER_ERROR) {
            return responseStatus.code();
        }
        if (responseStatus.value() != HttpStatus.INTERNAL_SERVER_ERROR) {
            return responseStatus.value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

