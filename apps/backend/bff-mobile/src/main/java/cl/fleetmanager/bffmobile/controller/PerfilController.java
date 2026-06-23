package cl.fleetmanager.bffmobile.controller;

import cl.fleetmanager.bffmobile.dto.PerfilResponse;
import cl.fleetmanager.bffmobile.entity.Conductor;
import cl.fleetmanager.bffmobile.repository.ConductorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
public class PerfilController {

    private final ConductorRepository conductorRepository;

    /**
     * GET /api/v1/mobile/perfil
     * Busca el conductor primero por AZURE_OID (claim "sub" — siempre presente),
     * luego por email como fallback.
     */
    @GetMapping("/perfil")
    public PerfilResponse obtenerPerfil(@AuthenticationPrincipal Jwt jwt) {
        String oid = jwt.getSubject();  // "sub" siempre viene en el token B2C
        log.info("JWT claims recibidos: sub={}, claims={}", oid, jwt.getClaims().keySet());
        log.info("emails={}, email={}, preferred_username={}",
            jwt.getClaim("emails"), jwt.getClaim("email"), jwt.getClaim("preferred_username"));

        // 1. Buscar por Azure OID (más confiable, no depende del claim email)
        if (oid != null && !oid.isBlank()) {
            var porOid = conductorRepository.findByAzureOidAndEliminado(oid, 0);
            if (porOid.isPresent()) {
                Conductor c = porOid.get();
                return new PerfilResponse(c.getId(), c.getNombre(), c.getEmail());
            }
        }

        // 2. Fallback: buscar por email
        String email = extraerEmail(jwt);
        if (email != null && !email.isBlank()) {
            return conductorRepository
                    .findByEmailAndEliminado(email, 0)
                    .map(c -> new PerfilResponse(c.getId(), c.getNombre(), c.getEmail()))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No se encontró conductor activo. oid=" + oid + " email=" + email));
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No se encontró conductor activo para oid=" + oid);
    }

    /** Azure B2C puede enviar el email en distintos claims según la configuración del User Flow. */
    private String extraerEmail(Jwt jwt) {
        Object emails = jwt.getClaim("emails");
        if (emails instanceof List<?> lista && !lista.isEmpty()) {
            String e = lista.get(0).toString();
            if (!e.isBlank()) return e;
        }
        String email = jwt.getClaim("email");
        if (email != null && !email.isBlank()) return email;
        String pref = jwt.getClaim("preferred_username");
        if (pref != null && !pref.isBlank() && pref.contains("@")) return pref;
        return null;
    }
}
