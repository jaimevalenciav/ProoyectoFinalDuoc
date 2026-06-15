package cl.fleetmanager.conductores.controller;

import cl.fleetmanager.conductores.entity.Conductor;
import cl.fleetmanager.conductores.repository.ConductorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoints para la app móvil.
 * El gateway BFF enruta /api/v1/mobile/** → ms-conductores:8083.
 * El gateway ya validó el JWT; aquí solo leemos el payload sin reverificar firma.
 */
@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
public class MobilePerfilController {

    private static final Logger log = LoggerFactory.getLogger(MobilePerfilController.class);

    private final ConductorRepository conductorRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/perfil")
    public Map<String, String> obtenerPerfil(HttpServletRequest request) {
        JwtClaims claims = extraerClaimsDeJwt(request);
        log.info("Buscando perfil — email={} oid={}", claims.email, claims.oid);

        // 1) Buscar por email
        if (claims.email != null && !claims.email.isBlank()) {
            Optional<Conductor> porEmail = conductorRepository
                    .findByEmailAndEliminado(claims.email, 0);
            if (porEmail.isPresent()) {
                return buildResponse(porEmail.get(), claims.email);
            }
        }

        // 2) Fallback: buscar por AZURE_OID (columna sin FK)
        if (claims.oid != null && !claims.oid.isBlank()) {
            Optional<Conductor> porAzureOid = conductorRepository
                    .findByAzureOidAndEliminado(claims.oid, 0);
            if (porAzureOid.isPresent()) {
                return buildResponse(porAzureOid.get(), claims.email);
            }

            // 3) Fallback adicional: buscar por usuarioId interno
            Optional<Conductor> porUsuarioId = conductorRepository
                    .findByUsuarioIdAndEliminado(claims.oid, 0);
            if (porUsuarioId.isPresent()) {
                return buildResponse(porUsuarioId.get(), claims.email);
            }
        }

        String detalle = "email=" + claims.email + " oid=" + claims.oid;
        log.warn("Conductor no encontrado: {}", detalle);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No existe conductor activo (" + detalle + ")");
    }

    private Map<String, String> buildResponse(Conductor c, String emailFallback) {
        String email = c.getEmail() != null ? c.getEmail()
                     : (emailFallback != null ? emailFallback : "");
        return Map.of(
                "conductorId", c.getId(),
                "nombre",      c.getNombre(),
                "email",       email
        );
    }

    /**
     * Decodifica el payload del JWT (sin verificar firma —
     * el gateway ya lo verificó antes de reenviar la petición).
     */
    private JwtClaims extraerClaimsDeJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Cabecera Authorization ausente o malformada");
            return new JwtClaims(null, null);
        }

        try {
            String[] partes = authHeader.substring(7).split("\\.");
            if (partes.length < 2) return new JwtClaims(null, null);

            byte[] payloadBytes = Base64.getUrlDecoder().decode(padBase64(partes[1]));
            JsonNode payload = objectMapper.readTree(payloadBytes);
            log.debug("JWT claims: {}", payload);

            String email = null;

            // Azure B2C puede devolver "emails" (array) o "email" (string)
            JsonNode emails = payload.get("emails");
            if (emails != null && emails.isArray() && !emails.isEmpty()) {
                email = emails.get(0).asText();
            }
            if (email == null) {
                JsonNode emailNode = payload.get("email");
                if (emailNode != null && !emailNode.isNull()) email = emailNode.asText();
            }
            if (email == null) {
                JsonNode preferred = payload.get("preferred_username");
                if (preferred != null && preferred.asText().contains("@")) {
                    email = preferred.asText();
                }
            }

            // oid = Azure Object ID (siempre presente en tokens B2C)
            String oid = null;
            JsonNode oidNode = payload.get("oid");
            if (oidNode != null && !oidNode.isNull()) oid = oidNode.asText();
            if (oid == null) {
                // sub también puede usarse como identificador
                JsonNode subNode = payload.get("sub");
                if (subNode != null && !subNode.isNull()) oid = subNode.asText();
            }

            return new JwtClaims(email, oid);

        } catch (Exception e) {
            log.error("Error al decodificar JWT payload", e);
            return new JwtClaims(null, null);
        }
    }

    private String padBase64(String s) {
        return s + "=".repeat((4 - s.length() % 4) % 4);
    }

    private record JwtClaims(String email, String oid) {}
}
