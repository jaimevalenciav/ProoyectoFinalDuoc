package cl.fleetmanager.vehiculos.service;

import cl.fleetmanager.vehiculos.dto.GpsPosicionActualDto;
import cl.fleetmanager.vehiculos.entity.GpsTrack;
import cl.fleetmanager.vehiculos.entity.Vehiculo;
import cl.fleetmanager.vehiculos.repository.GpsTrackRepository;
import cl.fleetmanager.vehiculos.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GpsService {

    private final GpsTrackRepository gpsRepo;
    private final VehiculoRepository  vehRepo;

    /** Última posición de cada vehículo, enriquecida con datos del vehículo */
    public List<GpsPosicionActualDto> getPosicionesActuales(String empresaId) {
        List<GpsTrack> tracks = gpsRepo.findUltimasPosiciones(empresaId);

        // Cargar vehículos en batch
        List<String> vehiculoIds = tracks.stream()
            .map(GpsTrack::getVehiculoId).distinct().collect(Collectors.toList());
        Map<String, Vehiculo> vehiculos = vehRepo.findAllById(vehiculoIds)
            .stream().collect(Collectors.toMap(Vehiculo::getId, Function.identity()));

        return tracks.stream().map(t -> {
            Vehiculo v = vehiculos.get(t.getVehiculoId());
            GpsPosicionActualDto dto = new GpsPosicionActualDto();
            dto.setId(t.getId());
            dto.setEmpresaId(t.getEmpresaId());
            dto.setVehiculoId(t.getVehiculoId());
            dto.setConductorId(t.getConductorId());
            dto.setLatitud(t.getLatitud());
            dto.setLongitud(t.getLongitud());
            dto.setVelocidad(t.getVelocidad());
            dto.setRumbo(t.getRumbo());
            dto.setEstadoMotor(t.getEstadoMotor());
            dto.setOdometro(t.getOdometro());
            dto.setRecordedAt(t.getRecordedAt());
            if (v != null) {
                dto.setPatente(v.getPatente());
                dto.setMarca(v.getMarca());
                dto.setModelo(v.getModelo());
                dto.setEstadoVehiculo(v.getEstado());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /** Recorrido histórico de un vehículo */
    public List<GpsTrack> getRecorrido(String vehiculoId, LocalDateTime desde, LocalDateTime hasta) {
        return gpsRepo.findRecorrido(vehiculoId, desde, hasta);
    }
}
