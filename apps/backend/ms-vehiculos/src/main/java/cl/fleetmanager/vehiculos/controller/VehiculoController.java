package cl.fleetmanager.vehiculos.controller;

import cl.fleetmanager.vehiculos.dto.VehiculoDto;
import cl.fleetmanager.vehiculos.entity.Vehiculo;
import cl.fleetmanager.vehiculos.repository.UsuarioRepository;
import cl.fleetmanager.vehiculos.service.VehiculoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehiculos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class VehiculoController {

    private final UsuarioRepository usuarioRepo;

    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return usuarioRepo.findByAzureOid(jwt.getSubject())
            .map(u -> u.getEmpresaId())
            .orElse("EMP-001");
    }

    private final VehiculoService servicio;

    @GetMapping
    public Page<Vehiculo> obtenerTodos(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) String busqueda,
        @RequestParam(defaultValue = "0")  int pagina,
        @RequestParam(defaultValue = "20") int tamano
    ) {
        return servicio.obtenerTodos(empresaId(jwt), estado, busqueda, pagina, tamano);
    }

    @GetMapping("/{id}")
    public Vehiculo obtenerPorId(@PathVariable String id) {
        return servicio.obtenerPorId(id);
    }

    @GetMapping("/qr/{codigoQr}")
    public Vehiculo obtenerPorQr(@PathVariable String codigoQr) {
        return servicio.obtenerPorQr(codigoQr);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vehiculo crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VehiculoDto datos) {
        return servicio.crear(empresaId(jwt), datos);
    }

    @PutMapping("/{id}")
    public Vehiculo actualizar(@PathVariable String id, @RequestBody VehiculoDto datos) {
        return servicio.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }
}
