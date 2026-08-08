package com.smartserve.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.smartserve.order.entity.CustomerOrder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void mapsDomainExceptionsToExpectedHttpStatuses() {
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handleResourceNotFound(new ResourceNotFoundException("missing"), request).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.handleBadRequest(new BadRequestException("bad"), request).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                handler.handleForbidden(new ForbiddenException("denied"), request).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                handler.handleConflict(new ConflictException("duplicate"), request).getStatusCode());
    }

    @Test
    void badCredentialsNeverLeaksAuthenticationDetails() {
        var response = handler.handleBadCredentials(new BadCredentialsException("database said no"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid username or password", response.getBody().message());
    }

    @Test
    void optimisticLockFailureUsesClientSafeConflictMessage() {
        var exception = new ObjectOptimisticLockingFailureException(CustomerOrder.class, 10L);

        var response = handler.handleConflict(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Ticket was changed by another user", response.getBody().message());
    }

    @Test
    void validationFailureIncludesFieldLevelErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "name", "must not be blank"),
                new FieldError("request", "phone", "must be valid")
        ));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult
        );

        var response = handler.handleValidation(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().message());
        assertEquals("must not be blank", response.getBody().validationErrors().get("name"));
        assertEquals("must be valid", response.getBody().validationErrors().get("phone"));
    }

    @Test
    void malformedJsonUsesStableClientMessage() {
        var exception = new HttpMessageNotReadableException("parser details");

        var response = handler.handleUnreadableMessage(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request body", response.getBody().message());
    }

    @Test
    void unknownExceptionDoesNotLeakInternalMessage() {
        var response = handler.handleException(new RuntimeException("secret database details"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("Unexpected server error", response.getBody().message());
        assertEquals("/api/test", response.getBody().path());
        assertNull(response.getBody().validationErrors());
    }
}
