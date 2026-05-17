package de.podolak.tools.minijira.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceExceptionsTest {
    @Test
    void authenticationExceptionStoresMessage() {
        assertThat(new AuthenticationException("auth failed")).hasMessage("auth failed");
    }

    @Test
    void badRequestExceptionStoresMessage() {
        assertThat(new BadRequestException("bad request")).hasMessage("bad request");
    }

    @Test
    void notFoundExceptionStoresMessage() {
        assertThat(new NotFoundException("not found")).hasMessage("not found");
    }
}
