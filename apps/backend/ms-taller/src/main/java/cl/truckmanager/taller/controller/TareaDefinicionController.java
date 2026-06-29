package cl.truckmanager.taller.controller;

import cl.truckmanager.taller.dto.TareaDefinicionDto;
import cl.truckmanager.taller.entity.TareaDefinicion;
import cl.truckmanager.taller.entity.UsuarioSistema;
import cl.truckmanager.taller.repository.UsuarioRepository;
import cl.truckmanager.taller.service.TareaDefinicionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tareas-definicion")
@RequiredArgsConstructor
public class TareaDefinicionController {

    private final TareaDefinicionService servicio;
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
    public Page<TareaDefinicion> getAll(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "50") int tamano
    ) {
        return servicio.getAll(empresaId(jwt), pagina, tamano);
    }

    @GetMapping("/activos")
    public List<TareaDefinicion> getAllActivos(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getAllActivos(empresaId(jwt));
    }

    @GetMapping("/{id}")
    public TareaDefinicion getById(@PathVariable String id) {
        return servicio.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TareaDefinicion crear(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody TareaDefinicionDto dto) {
        return servicio.crear(empresaId(jwt), dto);
    }

    @PutMapping("/{id}")
    public TareaDefinicion actualizar(@PathVariable String id,
                                      @Valid @RequestBody TareaDefinicionDto dto) {
        return servicio.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }
}
