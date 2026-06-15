package cl.fleetmanager.almacen.controller;

import cl.fleetmanager.almacen.dto.AjusteStockDto;
import cl.fleetmanager.almacen.dto.IngresoFacturaDto;
import cl.fleetmanager.almacen.dto.RepuestoDto;
import cl.fleetmanager.almacen.entity.Repuesto;
import cl.fleetmanager.almacen.entity.UsuarioSistema;
import cl.fleetmanager.almacen.repository.UsuarioRepository;
import cl.fleetmanager.almacen.service.RepuestoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/repuestos")
@RequiredArgsConstructor
public class RepuestoController {

    private final RepuestoService servicio;
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
    public Page<Repuesto> getAll(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamano
    ) {
        return servicio.getAll(empresaId(jwt), categoria, search, pagina, tamano);
    }

    @GetMapping("/activos")
    public List<Repuesto> getAllActivos(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getAllActivos(empresaId(jwt));
    }

    @GetMapping("/bajo-stock")
    public List<Repuesto> getBajoStock(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getBajoStock(empresaId(jwt));
    }

    @GetMapping("/{id}")
    public Repuesto getById(@PathVariable String id) {
        return servicio.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Repuesto crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RepuestoDto dto) {
        return servicio.crear(empresaId(jwt), dto);
    }

    @PutMapping("/{id}")
    public Repuesto actualizar(@PathVariable String id, @Valid @RequestBody RepuestoDto dto) {
        return servicio.actualizar(id, dto);
    }

    @PostMapping("/{id}/ajuste")
    public Repuesto ajustarStock(@PathVariable String id, @RequestBody AjusteStockDto dto) {
        return servicio.ajustarStock(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }

    @PostMapping("/ingreso-factura")
    public java.util.Map<String, Object> ingresoFactura(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody IngresoFacturaDto dto) {
        return servicio.ingresoFactura(empresaId(jwt), dto);
    }
}
