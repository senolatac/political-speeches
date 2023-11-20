package com.fashiondigital.politicalspeeches.exception

import com.fashiondigital.politicalspeeches.model.ErrorMessage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime


@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(EvaluationServiceException::class)
    fun handleException(ex: EvaluationServiceException): ResponseEntity<ErrorMessage> {
        val errorMessage: ErrorMessage = ErrorMessage(
                errorCode = ex.errorCode,
                errorMessage = ex.message)
        return ResponseEntity<ErrorMessage>(errorMessage, HttpStatus.BAD_REQUEST)
    }
}
