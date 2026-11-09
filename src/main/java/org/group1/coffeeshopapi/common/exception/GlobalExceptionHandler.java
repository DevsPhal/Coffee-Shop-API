package org.group1.coffeeshopapi.common.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.response.ErrorResponse;
import org.group1.coffeeshopapi.common.response.ErrorResponse.FieldMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size:5MB}")
    private DataSize maxFileSize = DataSize.ofMegabytes(5);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Account is not verified yet", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request);
    }

    // Every invalid field gets its own entry, so the client can put each message under its input.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldMessage> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldMessage(error.getField(), fieldMessage(error)))
                .toList();
        String message = Stream.concat(
                        errors.stream().map(FieldMessage::message),
                        ex.getBindingResult().getGlobalErrors().stream().map(ObjectError::getDefaultMessage))
                .distinct()
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message.isBlank() ? "Validation failed" : message, request, errors);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "No endpoint found for " + request.getMethod() + " " + request.getRequestURI(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), request);
    }

    // A value of the wrong type (an unknown enum, a bad UUID or number) names the field and what was sent.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = jsonPath(mismatch.getPath());
            String message = invalidValueMessage(mismatch, field);
            return build(HttpStatus.BAD_REQUEST, message, request, List.of(new FieldMessage(field, message)));
        }
        return build(HttpStatus.BAD_REQUEST, "Malformed or missing request body", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue(), request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Required form part '" + ex.getRequestPartName() + "' is missing", request);
    }

    // A subclass of MultipartException, so it needs its own handler to avoid the "malformed" message.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE,
                "The file is too large. The maximum size is " + maxFileSize.toMegabytes() + " MB", request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartError(HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Malformed multipart request — make sure the request body is sent as multipart/form-data "
                        + "with a valid boundary (let your HTTP client set this header automatically)", request);
    }

    // Each database rule gets its own message; only a delete blocked by references says so.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                       HttpServletRequest request) {
        String state = sqlState(ex);
        if (state == null) {
            log.warn("Data integrity violation without SQL state on {} {}", request.getMethod(), request.getRequestURI(), ex);
            return build(HttpStatus.CONFLICT, "This change conflicts with existing data — please refresh and try again", request);
        }
        return switch (state) {
            case "23505" -> build(HttpStatus.CONFLICT,
                    "This information conflicts with an existing record — check for a duplicate value "
                            + "(e.g. email or phone number)",
                    request);
            case "23503" -> isMissingReference(ex)
                    ? build(HttpStatus.CONFLICT, "A referenced record no longer exists — please refresh and try again", request)
                    : build(HttpStatus.CONFLICT,
                    "This record can't be deleted because other data still refers to it — deactivate it instead",
                    request);
            case "22001" -> build(HttpStatus.BAD_REQUEST, "A value is too long — shorten it and try again", request);
            case "23502" -> build(HttpStatus.BAD_REQUEST, "A required value is missing", request);
            case "22003" -> build(HttpStatus.BAD_REQUEST, "A number is too large", request);
            case "23514" -> build(HttpStatus.BAD_REQUEST, "A value isn't allowed — check the entered values", request);
            default -> {
                log.warn("Data integrity violation {} on {} {}", state, request.getMethod(), request.getRequestURI(), ex);
                yield build(HttpStatus.CONFLICT, "This change conflicts with existing data — please refresh and try again", request);
            }
        };
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "A referenced record no longer exists — please refresh and try again", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        return build(status, message, request, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                List<FieldMessage> errors) {
        ErrorResponse body = ErrorResponse.builder()
                .status(status)
                .message(message)
                .path(request.getRequestURI())
                .errors(errors)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    // DTO messages read as sentences ("Name is required"). Bean Validation's defaults ("must not be null")
    // don't name the field, so it is added in front.
    private static String fieldMessage(FieldError error) {
        String message = error.getDefaultMessage();
        if (message == null || message.isBlank()) {
            return readable(error.getField()) + " is invalid";
        }
        return Character.isLowerCase(message.charAt(0)) ? readable(error.getField()) + " " + message : message;
    }

    private static String invalidValueMessage(MismatchedInputException mismatch, String field) {
        Class<?> target = mismatch.getTargetType();
        String sent = mismatch instanceof InvalidFormatException invalid && invalid.getValue() != null
                ? " '" + invalid.getValue() + "'"
                : "";
        if (target != null && target.isEnum()) {
            String allowed = Arrays.stream(target.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "));
            return "Invalid value" + sent + " for " + field + " — use one of: " + allowed;
        }
        return "Invalid value" + sent + " for " + field;
    }

    // "items[0].quantity" from Jackson's path of property names and list indexes.
    private static String jsonPath(List<JsonMappingException.Reference> path) {
        StringBuilder field = new StringBuilder();
        for (JsonMappingException.Reference reference : path) {
            if (reference.getFieldName() != null) {
                if (!field.isEmpty()) {
                    field.append('.');
                }
                field.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                field.append('[').append(reference.getIndex()).append(']');
            }
        }
        return field.toString();
    }

    // "contactPhone" or "items[0].quantity" -> "Contact phone" / "Quantity".
    private static String readable(String field) {
        String last = field.substring(field.lastIndexOf('.') + 1).replaceAll("\\[\\d+]", "");
        String words = last.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        return words.isEmpty() ? "Value" : Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static String sqlState(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
        }
        return null;
    }

    // Postgres words an insert/update that points at a missing row as "is not present in table".
    private static boolean isMissingReference(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains("is not present in table")) {
                return true;
            }
        }
        return false;
    }
}
