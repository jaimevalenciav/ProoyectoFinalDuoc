package cl.fleetmanager.operaciones.controller;

import cl.fleetmanager.operaciones.dto.ClienteDto;
import cl.fleetmanager.operaciones.entity.Cliente;
import cl.fleetmanager.operaciones.entity.UsuarioSistema;
import cl.fleetmanager.operaciones.repository.UsuarioRepository;
import cl.fleetmanager.operaciones.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService    servicio;
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
    public Page<Cliente> obtenerTodos(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0")  int pagina,
        @RequestParam(defaultValue = "20") int tamano
    ) {
        return servicio.getAll(empresaId(jwt), search, pagina, tamano);
    }

    @GetMapping("/select")
    public List<Cliente> obtenerParaSelect(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getAllActivos(empresaId(jwt));
    }

    @GetMapping("/{id}")
    public Cliente obtenerPorId(@PathVariable String id) {
        return servicio.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cliente crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ClienteDto datos) {
        return servicio.crear(empresaId(jwt), datos);
    }

    @PutMapping("/{id}")
    public Cliente actualizar(@PathVariable String id, @RequestBody ClienteDto datos) {
        return servicio.actualizar(id, datos);
    }
}
