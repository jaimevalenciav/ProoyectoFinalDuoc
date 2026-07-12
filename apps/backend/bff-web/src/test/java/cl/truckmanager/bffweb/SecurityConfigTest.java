package cl.truckmanager.bffweb;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityConfig — pruebas unitarias")
class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    @DisplayName("corsConfigurationSource permite todos los origenes")
    void corsConfigurationSource_permiteTodosOrigenes() {
        CorsConfigurationSource source = config.corsConfigurationSource();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build());

        CorsConfiguration cors = source.getCorsConfiguration(exchange);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns()).contains("*");
    }

    @Test
    @DisplayName("corsConfigurationSource permite metodos HTTP requeridos")
    void corsConfigurationSource_permiteMetodosHTTP() {
        CorsConfigurationSource source = config.corsConfigurationSource();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build());

        CorsConfiguration cors = source.getCorsConfiguration(exchange);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedMethods())
                .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }

    @Test
    @DisplayName("corsConfigurationSource tiene allowCredentials activo")
    void corsConfigurationSource_allowCredentials() {
        CorsConfigurationSource source = config.corsConfigurationSource();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build());

        CorsConfiguration cors = source.getCorsConfiguration(exchange);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowCredentials()).isTrue();
    }
}
