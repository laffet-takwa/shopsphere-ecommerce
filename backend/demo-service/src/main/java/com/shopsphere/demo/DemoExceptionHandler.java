package com.shopsphere.demo;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Single replacement for the six per-service {@code ApiExceptionHandler} classes.
 *
 * <p>Six unordered advices in one context would mean whichever was registered first silently
 * handled every failure, so one handler with the same behaviour is used instead. The contract is
 * unchanged: RFC 7807 problem details, field errors on validation failures, and no internal
 * detail in a 500.
 */
@RestControllerAdvice
public class DemoExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DemoExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        List<String> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).toList();
        problem.setProperty("errors", fields);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> parameterValidation(HandlerMethodValidationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception) {
        if (exception instanceof ErrorResponse error) {
            return ResponseEntity.status(error.getStatusCode()).body(error.getBody());
        }
        log.error("Unhandled request failure ({})", exception.getClass().getSimpleName());
        return ResponseEntity.internalServerError()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                        "The request could not be completed"));
    }
}
