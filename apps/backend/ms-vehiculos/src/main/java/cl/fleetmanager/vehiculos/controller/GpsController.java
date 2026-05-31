package cl.fleetmanager.vehiculos.controller;

import cl.fleetmanager.vehiculos.dto.GpsPosicionActualDto;
import cl.fleetmanager.vehiculos.entity.GpsTrack;
import cl.fleetmanager.vehiculos.entity.UsuarioSistema;
import cl.fleetmanager.vehiculos.repository.UsuarioRepository;
import cl.fleetmanager.vehiculos.service.GpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/gps")
@RequiredArgsConstructor
public class GpsController {

    private final GpsService      servicio;
    private final UsuarioRepository usuarioRepo;

    private String empresaId(Jwt jwt) {
        if (jwt == null) return "EMP-001";
        String id = jwt.getClaimAsString("extension_empresaId");
        if (id != null && !id.isBlank()) return id;
        return usuarioRepo.findByAzureOid(jwt.getSubject())
            .map(UsuarioSistema::getEmpresaId)
            .orElse("EMP-001");
    }

    /** Posición actual de todos los vehículos de la empresa */
    @GetMapping("/posiciones-actuales")
    public List<GpsPosicionActualDto> posicionesActuales(@AuthenticationPrincipal Jwt jwt) {
        return servicio.getPosicionesActuales(empresaId(jwt));
    }

    /** Recorrido histórico de un vehículo */
    @GetMapping("/recorrido")
    public List<GpsTrack> recorrido(
        @RequestParam String vehiculoId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        return servicio.getRecorrido(vehiculoId, desde, hasta);
    }
}
