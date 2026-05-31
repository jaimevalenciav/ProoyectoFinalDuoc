package cl.fleetmanager.vehiculos.controller;

import cl.fleetmanager.vehiculos.dto.*;
import cl.fleetmanager.vehiculos.entity.*;
import cl.fleetmanager.vehiculos.repository.UsuarioRepository;
import cl.fleetmanager.vehiculos.service.MaestrosVehiculoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MaestrosVehiculoController {

    private final MaestrosVehiculoService servicio;
    private final UsuarioRepository       usuarioRepo;

    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return usuarioRepo.findByAzureOid(jwt.getSubject()).map(u -> u.getEmpresaId()).orElse("EMP-001");
    }

    // ── Sucursales ───────────────────────────────────────────────

    @GetMapping("/api/v1/sucursales")
    public List<Sucursal> getSucursales(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getSucursales(empresaId(jwt));
    }

    @PostMapping("/api/v1/sucursales")
    @ResponseStatus(HttpStatus.CREATED)
    public Sucursal createSucursal(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SucursalDto dto) {
        return servicio.createSucursal(empresaId(jwt), dto);
    }

    @PutMapping("/api/v1/sucursales/{id}")
    public Sucursal updateSucursal(@PathVariable String id, @RequestBody SucursalDto dto) {
        return servicio.updateSucursal(id, dto);
    }

    @DeleteMapping("/api/v1/sucursales/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSucursal(@PathVariable String id) {
        servicio.deleteSucursal(id);
    }

    // ── Municipalidades ──────────────────────────────────────────

    @GetMapping("/api/v1/municipalidades")
    public List<Municipalidad> getMunicipalidades(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getMunicipalidades(empresaId(jwt));
    }

    @PostMapping("/api/v1/municipalidades")
    @ResponseStatus(HttpStatus.CREATED)
    public Municipalidad createMunicipalidad(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MunicipalidadDto dto) {
        return servicio.createMunicipalidad(empresaId(jwt), dto);
    }

    @PutMapping("/api/v1/municipalidades/{id}")
    public Municipalidad updateMunicipalidad(@PathVariable String id, @RequestBody MunicipalidadDto dto) {
        return servicio.updateMunicipalidad(id, dto);
    }

    @DeleteMapping("/api/v1/municipalidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMunicipalidad(@PathVariable String id) {
        servicio.deleteMunicipalidad(id);
    }

    // ── Aseguradoras ─────────────────────────────────────────────

    @GetMapping("/api/v1/aseguradoras")
    public List<Aseguradora> getAseguradoras(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getAseguradoras(empresaId(jwt));
    }

    @PostMapping("/api/v1/aseguradoras")
    @ResponseStatus(HttpStatus.CREATED)
    public Aseguradora createAseguradora(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AseguradoraDto dto) {
        return servicio.createAseguradora(empresaId(jwt), dto);
    }

    @PutMapping("/api/v1/aseguradoras/{id}")
    public Aseguradora updateAseguradora(@PathVariable String id, @RequestBody AseguradoraDto dto) {
        return servicio.updateAseguradora(id, dto);
    }

    @DeleteMapping("/api/v1/aseguradoras/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAseguradora(@PathVariable String id) {
        servicio.deleteAseguradora(id);
    }

    // ── Plantas Revisión ─────────────────────────────────────────

    @GetMapping("/api/v1/plantas-revision")
    public List<PlantaRevision> getPlantasRevision(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getPlantasRevision(empresaId(jwt));
    }

    @PostMapping("/api/v1/plantas-revision")
    @ResponseStatus(HttpStatus.CREATED)
    public PlantaRevision createPlantaRevision(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlantaRevisionDto dto) {
        return servicio.createPlantaRevision(empresaId(jwt), dto);
    }

    @PutMapping("/api/v1/plantas-revision/{id}")
    public PlantaRevision updatePlantaRevision(@PathVariable String id, @RequestBody PlantaRevisionDto dto) {
        return servicio.updatePlantaRevision(id, dto);
    }

    @DeleteMapping("/api/v1/plantas-revision/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlantaRevision(@PathVariable String id) {
        servicio.deletePlantaRevision(id);
    }
}
