package com.event.recruitment.intelligent_recruitment_system.exception;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 处理通用异常
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Response<?>> handleException(Exception ex) {
        return new ResponseEntity<>(new Response<>(500, ex.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
