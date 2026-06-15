package cl.fleetmanager.conductores.controller;

import cl.fleetmanager.conductores.dto.PerfilDto;
import cl.fleetmanager.conductores.dto.UsuarioDto;
import cl.fleetmanager.conductores.entity.UsuarioSistema;
import cl.fleetmanager.conductores.repository.UsuarioRepository;
import cl.fleetmanager.conductores.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService servicio;
    private final UsuarioRepository repo;

    // ── Resuelve empresa desde JWT ───────────────────────────────
    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return repo.findByAzureOid(jwt.getSubject())
                .map(UsuarioSistema::getEmpresaId)
                .orElse("EMP-001");
    }

    // ── Perfil del usuario autenticado ───────────────────────────
    @GetMapping("/perfil-actual")
    public PerfilDto perfilActual(@AuthenticationPrincipal Jwt jwt) {
        return servicio.perfilActual(empresaId(jwt), jwt);
    }

    // ── CRUD (requiere rol ADMIN en el frontend; aquí no validamos rol) ──
    @GetMapping
    public List<UsuarioSistema> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listar(empresaId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioSistema crear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UsuarioDto dto) {
        return servicio.crear(empresaId(jwt), dto);
    }

    @PutMapping("/{id}")
    public UsuarioSistema actualizar(
            @PathVariable String id,
            @Valid @RequestBody UsuarioDto dto) {
        return servicio.actualizar(id, dto);
    }

    @PatchMapping("/{id}/rol")
    public Map<String, String> cambiarRol(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String nuevoRol = body.get("rol");
        servicio.cambiarRol(id, nuevoRol);
        return Map.of("rol", nuevoRol);
    }

    @PatchMapping("/{id}/activo")
    public Map<String, Object> cambiarActivo(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        boolean activo = Boolean.parseBoolean(String.valueOf(body.get("activo")));
        servicio.cambiarActivo(id, activo);
        return Map.of("activo", activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }
}
