package de.fimatas.home.client.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@CommonsLog
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public void handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) throws HttpRequestMethodNotSupportedException {
        log.warn("HttpRequestMethodNotSupportedException: " + request.getMethod() + " " + request.getRequestURI());
        throw ex;
    }
}