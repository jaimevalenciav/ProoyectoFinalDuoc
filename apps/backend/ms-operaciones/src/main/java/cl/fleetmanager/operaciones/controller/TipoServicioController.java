package cl.fleetmanager.operaciones.controller;

import cl.fleetmanager.operaciones.dto.TipoServicioDto;
import cl.fleetmanager.operaciones.entity.TipoServicio;
import cl.fleetmanager.operaciones.entity.UsuarioSistema;
import cl.fleetmanager.operaciones.repository.UsuarioRepository;
import cl.fleetmanager.operaciones.service.TipoServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-servicio")
@RequiredArgsConstructor
public class TipoServicioController {

    private final TipoServicioService servicio;
    private final UsuarioRepository   usuarioRepo;

    private String empresaId(Jwt jwt) {
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return usuarioRepo.findByAzureOid(jwt.getSubject())
            .map(UsuarioSistema::getEmpresaId)
            .orElse("EMP-001");
    }

    @GetMapping
    public List<TipoServicio> obtenerTodos(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getAll(empresaId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TipoServicio crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TipoServicioDto datos) {
        return servicio.crear(empresaId(jwt), datos);
    }

    @PutMapping("/{id}")
    public TipoServicio actualizar(@PathVariable String id, @Valid @RequestBody TipoServicioDto datos) {
        return servicio.actualizar(id, datos);
    }
}
