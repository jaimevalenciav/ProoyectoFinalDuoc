package cl.fleetmanager.taller.controller;

import cl.fleetmanager.taller.dto.*;
import cl.fleetmanager.taller.entity.*;
import cl.fleetmanager.taller.repository.UsuarioRepository;
import cl.fleetmanager.taller.service.OrdenTrabajoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ordenes-trabajo")
@RequiredArgsConstructor
public class OrdenTrabajoController {

    private final OrdenTrabajoService servicio;
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
    public Page<OrdenTrabajo> obtenerTodos(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) String tipo,
        @RequestParam(required = false) String vehiculoId,
        @RequestParam(defaultValue = "0")  int pagina,
        @RequestParam(defaultValue = "20") int tamano
    ) {
        return servicio.obtenerTodos(empresaId(jwt), estado, tipo, vehiculoId, pagina, tamano);
    }

    @GetMapping("/{id}")
    public OrdenTrabajo obtenerPorId(@PathVariable String id) {
        return servicio.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenTrabajo crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrdenTrabajoDto datos) {
        return servicio.crear(empresaId(jwt), datos);
    }

    @PutMapping("/{id}")
    public OrdenTrabajo actualizar(@PathVariable String id, @RequestBody OrdenTrabajoDto datos) {
        return servicio.actualizar(id, datos);
    }

    @PostMapping("/{id}/cerrar")
    public OrdenTrabajo cerrar(@PathVariable String id, @RequestBody Map<String, Object> body) {
        BigDecimal costo = body.containsKey("costoManoObra")
            ? new BigDecimal(body.get("costoManoObra").toString()) : null;
        String notas = body.containsKey("notas") ? body.get("notas").toString() : null;
        return servicio.cerrar(id, costo, notas);
    }

    @PostMapping("/{id}/tareas")
    @ResponseStatus(HttpStatus.CREATED)
    public TareaOT agregarTarea(@PathVariable String id, @Valid @RequestBody TareaOTDto datos) {
        return servicio.agregarTarea(id, datos);
    }

    @PatchMapping("/{id}/tareas/{tareaId}/completar")
    public OrdenTrabajo completarTarea(@PathVariable String id, @PathVariable String tareaId) {
        return servicio.completarTarea(id, tareaId);
    }

    @DeleteMapping("/{id}/tareas/{tareaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarTarea(@PathVariable String id, @PathVariable String tareaId) {
        servicio.eliminarTarea(id, tareaId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }
}
