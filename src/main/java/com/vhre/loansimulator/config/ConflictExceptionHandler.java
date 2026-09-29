package com.vhre.loansimulator.config;

import com.vhre.base.core.exceptions.ApiErrorResponse;
import com.vhre.loansimulator.modules.loan.exception.InvalidSimulationStatusException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Extends the internal starter's {@code GlobalExceptionHandler} with the
 * conflict mappings this API needs, so these cases answer a clean HTTP 409
 * instead of falling into the generic 500 catch-all:
 *
 * <ul>
 *   <li>{@link DataIntegrityViolationException}: database integrity
 *       violations (unique constraints such as uq_customers_email /
 *       uq_customers_national_id, NOT NULL violations).</li>
 *   <li>{@link InvalidSimulationStatusException}: loan simulation lifecycle
 *       transitions not allowed by the business state machine.</li>
 * </ul>
 *
 * <p>Spring always selects the most specific handler, so this advice
 * complements (never replaces) the starter's one. The response body keeps
 * the starter's {@link ApiErrorResponse} contract.</p>
 */
@RestControllerAdvice
public class ConflictExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .details(List.of("La operación viola una restricción de integridad "
                        + "(valor único ya registrado o campo obligatorio ausente)."))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidSimulationStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSimulationStatus(
            InvalidSimulationStatusException ex, HttpServletRequest request) {

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .details(List.of(ex.getMessage()))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
