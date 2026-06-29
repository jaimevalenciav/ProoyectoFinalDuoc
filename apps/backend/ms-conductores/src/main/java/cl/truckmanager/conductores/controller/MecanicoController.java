package cl.truckmanager.conductores.controller;

import cl.truckmanager.conductores.dto.MecanicoDto;
import cl.truckmanager.conductores.entity.Mecanico;
import cl.truckmanager.conductores.entity.UsuarioSistema;
import cl.truckmanager.conductores.repository.UsuarioRepository;
import cl.truckmanager.conductores.service.MecanicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mecanicos")
@RequiredArgsConstructor
public class MecanicoController {

    private final MecanicoService  servicio;
    private final UsuarioRepository usuarioRepo;

    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return usuarioRepo.findByAzureOid(jwt.getSubject())
            .map(UsuarioSistema::getEmpresaId)
            .orElse("EMP-001");
    }

    /** GET /api/v1/mecanicos?busqueda=&activo=1&pagina=0&tamano=50 */
    @GetMapping
    public Page<Mecanico> obtenerTodos(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false)            String  busqueda,
        @RequestParam(required = false)            Integer activo,
        @RequestParam(defaultValue = "0")          int     pagina,
        @RequestParam(defaultValue = "50")         int     tamano) {
        return servicio.obtenerTodos(empresaId(jwt), busqueda, activo, pagina, tamano);
    }

    /** GET /api/v1/mecanicos/activos — lista plana para selects */
    @GetMapping("/activos")
    public List<Mecanico> obtenerActivos(@AuthenticationPrincipal Jwt jwt) {
        return servicio.obtenerActivos(empresaId(jwt));
    }

    /** GET /api/v1/mecanicos/{id} */
    @GetMapping("/{id}")
    public Mecanico obtenerPorId(@PathVariable String id) {
        return servicio.obtenerPorId(id);
    }

    /** POST /api/v1/mecanicos */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mecanico crear(@AuthenticationPrincipal Jwt jwt,
                          @Valid @RequestBody MecanicoDto dto) {
        return servicio.crear(empresaId(jwt), dto);
    }

    /** PUT /api/v1/mecanicos/{id} */
    @PutMapping("/{id}")
    public Mecanico actualizar(@PathVariable String id,
                               @Valid @RequestBody MecanicoDto dto) {
        return servicio.actualizar(id, dto);
    }

    /** DELETE /api/v1/mecanicos/{id} — soft-delete (activo=0) */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }
}
