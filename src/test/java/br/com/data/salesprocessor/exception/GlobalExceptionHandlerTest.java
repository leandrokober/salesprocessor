package br.com.data.salesprocessor.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleInvalidApiKey_deveRetornarUnauthorizedPadronizado() {
        HttpServletRequest request = request("/api/vendas");

        ResponseEntity<ApiErrorResponse> response = handler.handleInvalidApiKey(
                new InvalidApiKeyException(),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().error()).isEqualTo("Unauthorized");
        assertThat(response.getBody().message()).isEqualTo("Acesso negado: X-API-KEY invalida ou ausente");
        assertThat(response.getBody().path()).isEqualTo("/api/vendas");
    }

    @Test
    void handleUnreadableMessage_deveRetornarBadRequestPadronizado() {
        HttpServletRequest request = request("/api/vendas");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnreadableMessage(null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("JSON invalido ou mal formatado");
        assertThat(response.getBody().path()).isEqualTo("/api/vendas");
    }

    private HttpServletRequest request(String path) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        return request;
    }
}
