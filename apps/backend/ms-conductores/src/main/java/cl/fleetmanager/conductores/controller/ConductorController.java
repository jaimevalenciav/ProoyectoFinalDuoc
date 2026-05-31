package cl.fleetmanager.conductores.controller;

import cl.fleetmanager.conductores.dto.ConductorDto;
import cl.fleetmanager.conductores.entity.Conductor;
import cl.fleetmanager.conductores.entity.UsuarioSistema;
import cl.fleetmanager.conductores.repository.UsuarioRepository;
import cl.fleetmanager.conductores.service.ConductorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/conductores")
@RequiredArgsConstructor
public class ConductorController {

    private final ConductorService  servicio;
    private final UsuarioRepository usuarioRepo;

    /** Resuelve empresaId desde claim JWT; fallback EMP-001 para dev sin auth */
    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return usuarioRepo.findByAzureOid(jwt.getSubject())
            .map(UsuarioSistema::getEmpresaId)
            .orElse("EMP-001");
    }

    @GetMapping
    public Page<Conductor> obtenerTodos(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false)          String estado,
        @RequestParam(required = false)          String search,
        @RequestParam(defaultValue = "0")  int   pagina,
        @RequestParam(defaultValue = "50") int   tamano
    ) {
        return servicio.obtenerTodos(empresaId(jwt), estado, search, pagina, tamano);
    }

    @GetMapping("/{id}")
    public Conductor obtenerPorId(@PathVariable String id) {
        return servicio.obtenerPorId(id);
    }

    @GetMapping("/{id}/score")
    public Map<String, Object> obtenerScore(@PathVariable String id) {
        return servicio.obtenerScore(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Conductor crear(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ConductorDto datos
    ) {
        return servicio.crear(empresaId(jwt), datos);
    }

    @PutMapping("/{id}")
    public Conductor actualizar(
        @PathVariable String id,
        @RequestBody ConductorDto datos
    ) {
        return servicio.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }
}
