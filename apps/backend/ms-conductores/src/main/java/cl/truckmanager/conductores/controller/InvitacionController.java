package cl.truckmanager.conductores.controller;

import cl.truckmanager.conductores.dto.InvitacionDto;
import cl.truckmanager.conductores.dto.InvitacionResumenDto;
import cl.truckmanager.conductores.dto.PerfilDto;
import cl.truckmanager.conductores.entity.UsuarioSistema;
import cl.truckmanager.conductores.repository.UsuarioRepository;
import cl.truckmanager.conductores.service.InvitacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitaciones")
@RequiredArgsConstructor
public class InvitacionController {

    private final InvitacionService servicio;
    private final UsuarioRepository repo;

    // ── Helpers ──────────────────────────────────────────────────
    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return repo.findByAzureOid(jwt.getSubject())
                .map(UsuarioSistema::getEmpresaId)
                .orElse("EMP-001");
    }

    // ── Crear invitación (ADMIN) ──────────────────────────────────
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitacionResumenDto crear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InvitacionDto dto) {
        return servicio.crear(empresaId(jwt), dto);
    }

    // ── Listar invitaciones de la empresa ─────────────────────────
    @GetMapping
    public List<InvitacionResumenDto> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listar(empresaId(jwt));
    }

    // ── Validar token (sin autenticación, usado en onboarding) ────
    @GetMapping("/{token}/validar")
    public InvitacionResumenDto validar(@PathVariable String token) {
        return servicio.validar(token);
    }

    // ── Aceptar invitación (usuario autenticado recién registrado) ─
    @PostMapping("/{token}/aceptar")
    public PerfilDto aceptar(
            @PathVariable String token,
            @AuthenticationPrincipal Jwt jwt) {
        return servicio.aceptar(token, jwt);
    }

    // ── Revocar invitación ────────────────────────────────────────
    @DeleteMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revocar(@PathVariable String token) {
        servicio.revocar(token);
    }
}
