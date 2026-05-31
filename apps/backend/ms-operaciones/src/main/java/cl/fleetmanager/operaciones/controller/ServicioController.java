package cl.fleetmanager.operaciones.controller;

import cl.fleetmanager.operaciones.dto.AsignarServicioDto;
import cl.fleetmanager.operaciones.dto.ServicioDto;
import cl.fleetmanager.operaciones.entity.Servicio;
import cl.fleetmanager.operaciones.entity.UsuarioSistema;
import cl.fleetmanager.operaciones.repository.UsuarioRepository;
import cl.fleetmanager.operaciones.service.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService   servicio;
    private final UsuarioRepository usuarioRepo;

    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return usuarioRepo.findByAzureOid(jwt.getSubject())
            .map(UsuarioSistema::getEmpresaId)
            .orElse("EMP-001");
    }

    @GetMapping
    public Page<Servicio> obtenerTodos(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) String clienteId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(required = false) Integer facturado,
        @RequestParam(defaultValue = "0")  int pagina,
        @RequestParam(defaultValue = "20") int tamano
    ) {
        return servicio.getAll(empresaId(jwt), estado, clienteId, desde, hasta, facturado, pagina, tamano);
    }

    @GetMapping("/{id}")
    public Servicio obtenerPorId(@PathVariable String id) {
        return servicio.getById(id);
    }

    /** Viajes asignados a un conductor — usado por la app móvil */
    @GetMapping("/conductor/{conductorId}")
    public List<Servicio> viajesConductor(@PathVariable String conductorId) {
        return servicio.viajesConductor(conductorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Servicio crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ServicioDto datos) {
        return servicio.crear(empresaId(jwt), datos);
    }

    @PutMapping("/{id}")
    public Servicio actualizar(@PathVariable String id, @RequestBody ServicioDto datos) {
        return servicio.actualizar(id, datos);
    }

    /** Aprobar: BORRADOR/PENDIENTE → APROBADO */
    @PatchMapping("/{id}/aprobar")
    public Servicio aprobar(@PathVariable String id) {
        return servicio.cambiarEstado(id, "APROBADO", List.of("BORRADOR", "PENDIENTE"));
    }

    /** Asignar vehículo y conductor (solo si APROBADO) */
    @PatchMapping("/{id}/asignar")
    public Servicio asignar(@PathVariable String id, @RequestBody AsignarServicioDto dto) {
        return servicio.asignar(id, dto);
    }

    /** Iniciar viaje: APROBADO → EN_CURSO */
    @PatchMapping("/{id}/iniciar")
    public Servicio iniciar(@PathVariable String id) {
        return servicio.cambiarEstado(id, "EN_CURSO", List.of("APROBADO"));
    }

    /** Completar viaje: EN_CURSO → COMPLETADO */
    @PatchMapping("/{id}/completar")
    public Servicio completar(@PathVariable String id) {
        return servicio.cambiarEstado(id, "COMPLETADO", List.of("EN_CURSO"));
    }

    /** Cancelar desde cualquier estado no terminal */
    @PatchMapping("/{id}/cancelar")
    public Servicio cancelar(@PathVariable String id) {
        return servicio.cambiarEstado(id, "CANCELADO",
            List.of("BORRADOR", "PENDIENTE", "APROBADO", "EN_CURSO"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }
}
