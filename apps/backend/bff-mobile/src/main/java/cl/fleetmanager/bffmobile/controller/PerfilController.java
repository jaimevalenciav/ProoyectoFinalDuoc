package cl.fleetmanager.bffmobile.controller;

import cl.fleetmanager.bffmobile.dto.PerfilResponse;
import cl.fleetmanager.bffmobile.entity.Conductor;
import cl.fleetmanager.bffmobile.repository.ConductorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
public class PerfilController {

    private final ConductorRepository conductorRepository;

    /**
     * GET /api/v1/mobile/perfil
     * Devuelve el perfil del conductor asociado al email del JWT de Azure B2C.
     */
    @GetMapping("/perfil")
    public PerfilResponse obtenerPerfil(@AuthenticationPrincipal Jwt jwt) {
        String email = extraerEmail(jwt);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token no contiene un email válido");
        }

        Conductor conductor = conductorRepository
                .findByEmailAndEliminado(email, 0)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un conductor activo con el email: " + email));

        return new PerfilResponse(conductor.getId(), conductor.getNombre(), conductor.getEmail());
    }

    /** Azure B2C puede enviar el email como "emails" (lista) o "email" (string). */
    private String extraerEmail(Jwt jwt) {
        // Claim "emails" — lista en B2C
        Object emails = jwt.getClaim("emails");
        if (emails instanceof List<?> lista && !lista.isEmpty()) {
            return lista.get(0).toString();
        }
        // Claim "email" — string
        String email = jwt.getClaim("email");
        if (email != null) return email;
        // Fallback: preferred_username
        return jwt.getClaim("preferred_username");
    }
}
