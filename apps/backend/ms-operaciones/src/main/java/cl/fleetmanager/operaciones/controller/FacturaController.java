package cl.fleetmanager.operaciones.controller;

import cl.fleetmanager.operaciones.dto.FacturarDto;
import cl.fleetmanager.operaciones.entity.Factura;
import cl.fleetmanager.operaciones.entity.UsuarioSistema;
import cl.fleetmanager.operaciones.repository.UsuarioRepository;
import cl.fleetmanager.operaciones.service.FacturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService    servicio;
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
    public Page<Factura> obtenerTodas(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String clienteId,
        @RequestParam(required = false) String estado,
        @RequestParam(defaultValue = "0")  int pagina,
        @RequestParam(defaultValue = "20") int tamano
    ) {
        return servicio.getAll(empresaId(jwt), clienteId, estado, pagina, tamano);
    }

    @GetMapping("/{id}")
    public Factura obtenerPorId(@PathVariable String id) {
        return servicio.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Factura facturar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody FacturarDto datos) {
        return servicio.facturar(empresaId(jwt), datos);
    }

    @PostMapping("/{id}/anular")
    public Factura anular(@PathVariable String id) {
        return servicio.anular(id);
    }
}
