package cl.truckmanager.operaciones.controller;

import cl.truckmanager.operaciones.dto.CargaAdBlueDto;
import cl.truckmanager.operaciones.entity.CargaAdBlue;
import cl.truckmanager.operaciones.entity.UsuarioSistema;
import cl.truckmanager.operaciones.repository.UsuarioRepository;
import cl.truckmanager.operaciones.service.CargaAdBlueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/adblue")
@RequiredArgsConstructor
public class CargaAdBlueController {

    private final CargaAdBlueService servicio;
    private final UsuarioRepository  usuarioRepo;

    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return usuarioRepo.findByAzureOid(jwt.getSubject())
            .map(UsuarioSistema::getEmpresaId)
            .orElse("EMP-001");
    }

    @GetMapping("/cargas")
    public Page<CargaAdBlue> obtenerCargas(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String vehiculoId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(defaultValue = "0")  int pagina,
        @RequestParam(defaultValue = "50") int tamano
    ) {
        return servicio.getAll(empresaId(jwt), vehiculoId, desde, hasta, pagina, tamano);
    }

    @GetMapping("/cargas/{id}")
    public CargaAdBlue obtenerPorId(@PathVariable String id) {
        return servicio.getById(id);
    }

    @PostMapping("/cargas")
    @ResponseStatus(HttpStatus.CREATED)
    public CargaAdBlue registrar(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody CargaAdBlueDto dto
    ) {
        return servicio.registrar(empresaId(jwt), dto);
    }

    @DeleteMapping("/cargas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        servicio.eliminar(id);
    }

    @GetMapping("/ultima-carga/{vehiculoId}")
    public CargaAdBlue ultimaCarga(@PathVariable String vehiculoId) {
        return servicio.getUltimaCarga(vehiculoId).orElse(null);
    }

    @GetMapping("/anomalias")
    public List<CargaAdBlue> anomalias(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getAnomalias(empresaId(jwt));
    }
}
