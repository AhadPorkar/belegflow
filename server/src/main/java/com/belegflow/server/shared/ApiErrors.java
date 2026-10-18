package com.belegflow.server.shared;
import com.belegflow.domain.BusinessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
@RestControllerAdvice public class ApiErrors {
    private static final Set<String> FORBIDDEN=Set.of("FORBIDDEN","NOT_ASSIGNED","SELF_APPROVAL","SECOND_APPROVAL_ADMIN_ONLY","DISTINCT_APPROVERS_REQUIRED");
    @ExceptionHandler(BusinessException.class) ResponseEntity<?> business(BusinessException e) {
        int status=Set.of("AUTH_REQUIRED","SESSION_REVOKED").contains(e.code())?401:e.code().equals("NOT_FOUND")?404:e.code().equals("VERSION_CONFLICT")||e.code().equals("IDEMPOTENCY_CONFLICT")?409:FORBIDDEN.contains(e.code())?403:422;
        return ResponseEntity.status(status).body(Map.of("code",e.code()));
    }
    @ExceptionHandler( {
        org.springframework.web.bind.MissingServletRequestParameterException.class,org.springframework.web.multipart.support.MissingServletRequestPartException.class,MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,IllegalArgumentException.class
    }) ResponseEntity<?> validation(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("code","INVALID_INPUT"));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> size(Exception e) {
        return ResponseEntity.status(413).body(Map.of("code","INVALID_FILE"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict(Exception e) {
        return ResponseEntity.status(409).body(Map.of("code","DATA_CONFLICT"));
    }
    @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(Exception e) {
        String reference=UUID.randomUUID().toString();
        // Do not log exception messages: JDBC messages may contain personal/document data.
        LoggerFactory.getLogger(ApiErrors.class).error("Request failed reference={} exception={}",reference,e.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(Map.of("code","UNEXPECTED_ERROR","reference",reference));
    }
}
