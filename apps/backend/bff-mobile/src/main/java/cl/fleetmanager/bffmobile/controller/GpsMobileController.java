package cl.fleetmanager.bffmobile.controller;

import cl.fleetmanager.bffmobile.dto.GpsTrackRequest;
import cl.fleetmanager.bffmobile.entity.GpsTrack;
import cl.fleetmanager.bffmobile.entity.Vehiculo;
import cl.fleetmanager.bffmobile.repository.GpsTrackRepository;
import cl.fleetmanager.bffmobile.repository.VehiculoRepository;
import cl.fleetmanager.bffmobile.service.GpsPublisherService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/gps")
@RequiredArgsConstructor
public class GpsMobileController {

    private final GpsPublisherService  publicador;
    private final GpsTrackRepository   gpsTrackRepo;
    private final VehiculoRepository   vehiculoRepo;

    /**
     * POST /api/v1/mobile/gps/track
     * Recibe posición GPS desde la app móvil KMP.
     * 1) Publica en Service Bus (para fn-gps-procesador → PISTAS_GPS)
     * 2) Guarda directo en GPS_TRACKS vía JPA (para el mapa web)
     */
    @PostMapping("/track")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Transactional
    public Map<String, Object> registrarTrack(@RequestBody PistaGpsMobile cuerpo) {
        log.info("GPS track recibido conductor={} vehiculo={} lat={} lon={}",
                cuerpo.getConductorId(), cuerpo.getVehiculoId(),
                cuerpo.getLatitud(), cuerpo.getLongitud());

        // ── 1. Publicar en Service Bus ────────────────────────────────────
        GpsTrackRequest req = new GpsTrackRequest();
        req.setIdConductor(cuerpo.getConductorId());
        req.setIdVehiculo(cuerpo.getVehiculoId());
        req.setLatitud(cuerpo.getLatitud());
        req.setLongitud(cuerpo.getLongitud());
        req.setVelocidad(cuerpo.getVelocidad());
        req.setPrecision(cuerpo.getPrecision());
        req.setRegistradoEn(cuerpo.getRecordedAt());
        publicador.publicar(req);

        // ── 2. Guardar en GPS_TRACKS (mapa web) ──────────────────────────
        try {
            vehiculoRepo.findById(cuerpo.getVehiculoId()).ifPresentOrElse(
                vehiculo -> guardarGpsTrack(cuerpo, vehiculo),
                () -> log.warn("Vehículo no encontrado para GPS track: {}", cuerpo.getVehiculoId())
            );
        } catch (Exception e) {
            log.error("Error al guardar GPS_TRACKS (Service Bus OK): {}", e.getMessage());
        }

        return Map.of(
            "id",         java.util.UUID.randomUUID().toString(),
            "recordedAt", cuerpo.getRecordedAt() != null ? cuerpo.getRecordedAt() : ""
        );
    }

    private void guardarGpsTrack(PistaGpsMobile cuerpo, Vehiculo vehiculo) {
        GpsTrack track = new GpsTrack();
        track.setEmpresaId(vehiculo.getEmpresaId());
        track.setVehiculoId(cuerpo.getVehiculoId());
        track.setConductorId(cuerpo.getConductorId());
        track.setLatitud(bd(cuerpo.getLatitud(), 7));
        track.setLongitud(bd(cuerpo.getLongitud(), 7));
        if (cuerpo.getVelocidad() != null)
            track.setVelocidad(bd(cuerpo.getVelocidad() * 3.6, 1)); // m/s → km/h
        if (cuerpo.getPrecision() != null)
            track.setPrecisionM(bd(cuerpo.getPrecision(), 1));
        gpsTrackRepo.saveAndFlush(track);
        log.info("GPS_TRACKS OK — vehiculo={} empresa={}", cuerpo.getVehiculoId(), vehiculo.getEmpresaId());
    }

    private static BigDecimal bd(Double valor, int escala) {
        return new BigDecimal(valor).setScale(escala, RoundingMode.HALF_UP);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PistaGpsMobile {
        private String conductorId;
        private String vehiculoId;
        private Double latitud;
        private Double longitud;
        private Double velocidad;
        private Double precision;
        private String recordedAt;
    }
}
