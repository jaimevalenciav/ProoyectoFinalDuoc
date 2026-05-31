package cl.fleetmanager.operaciones.controller;

import cl.fleetmanager.operaciones.dto.AlertaCombustibleDto;
import cl.fleetmanager.operaciones.dto.CargaCombustibleDto;
import cl.fleetmanager.operaciones.entity.AlertaCombustible;
import cl.fleetmanager.operaciones.entity.CargaCombustible;
import cl.fleetmanager.operaciones.entity.UsuarioSistema;
import cl.fleetmanager.operaciones.repository.UsuarioRepository;
import cl.fleetmanager.operaciones.service.AlertaCombustibleService;
import cl.fleetmanager.operaciones.service.CargaCombustibleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/combustible")
@RequiredArgsConstructor
public class CargaCombustibleController {

    private final CargaCombustibleService  servicio;
    private final AlertaCombustibleService alertaServicio;
    private final UsuarioRepository        usuarioRepo;

    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return usuarioRepo.findByAzureOid(jwt.getSubject())
            .map(UsuarioSistema::getEmpresaId)
            .orElse("EMP-001");
    }

    @GetMapping("/cargas")
    public Page<CargaCombustible> obtenerCargas(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String vehiculoId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(defaultValue = "0")  int pagina,
        @RequestParam(defaultValue = "50") int tamano
    ) {
        return servicio.getAll(empresaId(jwt), vehiculoId, desde, hasta, pagina, tamano);
    }

    @GetMapping("/cargas/{id}")
    public CargaCombustible obtenerPorId(@PathVariable String id) {
        return servicio.getById(id);
    }

    @PostMapping("/cargas")
    @ResponseStatus(HttpStatus.CREATED)
    public CargaCombustible registrar(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody CargaCombustibleDto dto
    ) {
        return servicio.registrar(empresaId(jwt), dto);
    }

    @DeleteMapping("/cargas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }

    /** Última carga de un vehículo — para validaciones en tiempo real del formulario */
    @GetMapping("/ultima-carga/{vehiculoId}")
    public CargaCombustible ultimaCarga(@PathVariable String vehiculoId) {
        return servicio.getUltimaCarga(vehiculoId).orElse(null);
    }

    @GetMapping("/anomalias")
    public List<CargaCombustible> anomalias(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getAnomalias(empresaId(jwt));
    }

    // ── Alertas de combustible ────────────────────────────────────────────────

    /**
     * Guarda alertas generadas al registrar una carga.
     * Body: lista de AlertaCombustibleDto (solo warning/info; los error bloquean el guardado).
     */
    @PostMapping("/alertas")
    @ResponseStatus(HttpStatus.CREATED)
    public void guardarAlertas(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody List<AlertaCombustibleDto> alertas
    ) {
        alertaServicio.guardar(empresaId(jwt), alertas);
    }

    /**
     * Lista de alertas.
     * @param activas true → solo activas (no leídas); false → solo historial; omitido → todas
     */
    @GetMapping("/alertas")
    public List<AlertaCombustible> getAlertas(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) Boolean activas
    ) {
        return alertaServicio.getAlertas(empresaId(jwt), activas);
    }

    /**
     * Marca una alerta como leída. Body: { "leidaPor": "Nombre Usuario" }
     */
    @PatchMapping("/alertas/{id}/leida")
    public AlertaCombustible marcarLeida(
        @PathVariable String id,
        @RequestBody Map<String, String> body
    ) {
        return alertaServicio.marcarLeida(id, body.get("leidaPor"));
    }
}
