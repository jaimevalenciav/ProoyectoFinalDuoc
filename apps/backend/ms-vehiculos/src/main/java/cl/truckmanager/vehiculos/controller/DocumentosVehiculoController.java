package cl.truckmanager.vehiculos.controller;

import cl.truckmanager.vehiculos.dto.*;
import cl.truckmanager.vehiculos.entity.*;
import cl.truckmanager.vehiculos.repository.UsuarioRepository;
import cl.truckmanager.vehiculos.service.DocumentosVehiculoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DocumentosVehiculoController {

    private final DocumentosVehiculoService servicio;
    private final UsuarioRepository         usuarioRepo;

    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        Optional<UsuarioSistema> found = usuarioRepo.findByAzureOid(jwt.getSubject());
        return found.isPresent() ? found.get().getEmpresaId() : "EMP-001";
    }

    // ── Permiso Circulación ──────────────────────────────────────

    @GetMapping("/api/v1/permisos-circulacion")
    public List<PermisoCirculacion> getPermisos(@RequestParam String vehiculoId) {
        return servicio.getPermisos(vehiculoId);
    }

    @PostMapping("/api/v1/permisos-circulacion")
    @ResponseStatus(HttpStatus.CREATED)
    public PermisoCirculacion createPermiso(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PermisoCirculacionDto dto) {
        return servicio.createPermiso(empresaId(jwt), dto);
    }

    @DeleteMapping("/api/v1/permisos-circulacion/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermiso(@PathVariable String id) {
        servicio.deletePermiso(id);
    }

    // ── Seguro SOAP ──────────────────────────────────────────────

    @GetMapping("/api/v1/seguros-soap")
    public List<SeguroSoap> getSeguros(@RequestParam String vehiculoId) {
        return servicio.getSeguros(vehiculoId);
    }

    @PostMapping("/api/v1/seguros-soap")
    @ResponseStatus(HttpStatus.CREATED)
    public SeguroSoap createSeguro(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SeguroSoapDto dto) {
        return servicio.createSeguro(empresaId(jwt), dto);
    }

    @DeleteMapping("/api/v1/seguros-soap/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSeguro(@PathVariable String id) {
        servicio.deleteSeguro(id);
    }

    // ── Revisión Técnica ─────────────────────────────────────────

    @GetMapping("/api/v1/revisiones-tecnicas")
    public List<RevisionTecnica> getRevisiones(@RequestParam String vehiculoId) {
        return servicio.getRevisiones(vehiculoId);
    }

    @PostMapping("/api/v1/revisiones-tecnicas")
    @ResponseStatus(HttpStatus.CREATED)
    public RevisionTecnica createRevision(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RevisionTecnicaDto dto) {
        return servicio.createRevision(empresaId(jwt), dto);
    }

    @DeleteMapping("/api/v1/revisiones-tecnicas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRevision(@PathVariable String id) {
        servicio.deleteRevision(id);
    }
}
