package de.podolak.tools.minijira.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.podolak.tools.minijira.service.AuthenticationException;
import de.podolak.tools.minijira.service.BadRequestException;
import de.podolak.tools.minijira.service.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {
    @Test
    void handleValidationReturnsBadRequestWithFieldViolations() throws NoSuchMethodException {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "title", "must not be blank"));
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validationTarget", Object.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleValidation(exception, request("/api/issues"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().path()).isEqualTo("/api/issues");
        assertThat(response.getBody().violations()).singleElement()
                .satisfies(violation -> {
                    assertThat(violation.field()).isEqualTo("title");
                    assertThat(violation.message()).isEqualTo("must not be blank");
                });
    }

    @Test
    void handleBadRequestReturnsBadRequest() {
        var response = new GlobalExceptionHandler()
                .handleBadRequest(new BadRequestException("bad input"), request("/api/users"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("bad input");
        assertThat(response.getBody().path()).isEqualTo("/api/users");
        assertThat(response.getBody().violations()).isEmpty();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void handleAuthReturnsUnauthorized() {
        var response = new GlobalExceptionHandler()
                .handleAuth(new AuthenticationException("not logged in"), request("/api/session"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().message()).isEqualTo("not logged in");
    }

    @Test
    void handleNotFoundReturnsNotFound() {
        var response = new GlobalExceptionHandler()
                .handleNotFound(new NotFoundException("missing"), request("/api/issues/1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("missing");
    }

    @Test
    void handleIllegalArgumentReturnsBadRequest() {
        var response = new GlobalExceptionHandler()
                .handleIllegalArgument(new IllegalArgumentException("bad enum"), request("/api/issues"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("bad enum");
    }

    @SuppressWarnings("unused")
    private static void validationTarget(@Valid Object request) {
    }

    private static HttpServletRequest request(String uri) {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }
}
