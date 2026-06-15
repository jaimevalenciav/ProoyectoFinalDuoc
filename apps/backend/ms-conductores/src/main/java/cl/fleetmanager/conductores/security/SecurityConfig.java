package cl.fleetmanager.conductores.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ms-conductores es un servicio interno: el browser nunca lo llama directamente,
 * sólo lo hace el BFF (gateway). Por eso NO se configura CORS aquí —
 * el BFF es el único responsable de añadir los headers CORS al cliente.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
            // Sin oauth2ResourceServer: el gateway (bff-web) ya validó el JWT.
            // ms-conductores es un servicio interno de confianza.
        return http.build();
    }
}
