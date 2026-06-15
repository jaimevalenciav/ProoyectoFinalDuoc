package cl.fleetmanager.almacen.controller;

import cl.fleetmanager.almacen.entity.MovimientoStock;
import cl.fleetmanager.almacen.entity.UsuarioSistema;
import cl.fleetmanager.almacen.repository.MovimientoStockRepository;
import cl.fleetmanager.almacen.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/movimientos-stock")
@RequiredArgsConstructor
public class MovimientoStockController {

    private final MovimientoStockRepository repo;
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
    public Page<MovimientoStock> getAll(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String repuestoId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "30") int tamano
    ) {
        LocalDateTime desdeTs = desde != null && !desde.isBlank()
                ? LocalDate.parse(desde).atStartOfDay() : null;
        LocalDateTime hastaTs = hasta != null && !hasta.isBlank()
                ? LocalDate.parse(hasta).atTime(23, 59, 59) : null;

        return repo.buscar(
                empresaId(jwt),
                repuestoId != null && !repuestoId.isBlank() ? repuestoId : null,
                tipo       != null && !tipo.isBlank()       ? tipo.toUpperCase() : null,
                documento  != null && !documento.isBlank()  ? documento : null,
                desdeTs,
                hastaTs,
                PageRequest.of(pagina, tamano)
        );
    }
}
