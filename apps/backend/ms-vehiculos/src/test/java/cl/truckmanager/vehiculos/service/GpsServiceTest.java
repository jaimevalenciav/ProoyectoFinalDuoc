package cl.truckmanager.vehiculos.service;

import cl.truckmanager.vehiculos.dto.GpsPosicionActualDto;
import cl.truckmanager.vehiculos.dto.GpsTrackMobileRequest;
import cl.truckmanager.vehiculos.entity.GpsTrack;
import cl.truckmanager.vehiculos.entity.Vehiculo;
import cl.truckmanager.vehiculos.repository.GpsTrackRepository;
import cl.truckmanager.vehiculos.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GpsService — pruebas unitarias")
class GpsServiceTest {

    @Mock
    private GpsTrackRepository gpsRepo;

    @Mock
    private VehiculoRepository vehRepo;

    @InjectMocks
    private GpsService servicio;

    private static final String EMPRESA_ID  = "EMP-001";
    private static final String VEHICULO_ID = "VEH-001";

    private GpsTrack trackEjemplo;
    private Vehiculo vehiculoEjemplo;

    @BeforeEach
    void configurar() {
        trackEjemplo = new GpsTrack();
        trackEjemplo.setId(1L);
        trackEjemplo.setEmpresaId(EMPRESA_ID);
        trackEjemplo.setVehiculoId(VEHICULO_ID);
        trackEjemplo.setLatitud(new BigDecimal("-33.4569"));
        trackEjemplo.setLongitud(new BigDecimal("-70.6483"));
        trackEjemplo.setVelocidad(new BigDecimal("60.0"));
        trackEjemplo.setRecordedAt(LocalDateTime.now());

        vehiculoEjemplo = new Vehiculo();
        vehiculoEjemplo.setId(VEHICULO_ID);
        vehiculoEjemplo.setEmpresaId(EMPRESA_ID);
        vehiculoEjemplo.setPatente("BGJK-91");
        vehiculoEjemplo.setMarca("Volvo");
        vehiculoEjemplo.setModelo("FH16");
        vehiculoEjemplo.setEstado("OPERATIVO");
    }

    // ─── getPosicionesActuales ────────────────────────────────────────────────

    @Test
    @DisplayName("getPosicionesActuales retorna lista enriquecida con datos del vehiculo")
    void getPosicionesActuales_conTracks_retornaListaEnriquecida() {
        when(gpsRepo.findUltimasPosiciones(EMPRESA_ID)).thenReturn(List.of(trackEjemplo));
        when(vehRepo.findAllById(anyList())).thenReturn(List.of(vehiculoEjemplo));

        List<GpsPosicionActualDto> resultado = servicio.getPosicionesActuales(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getVehiculoId()).isEqualTo(VEHICULO_ID);
        assertThat(resultado.get(0).getPatente()).isEqualTo("BGJK-91");
        assertThat(resultado.get(0).getMarca()).isEqualTo("Volvo");
    }

    @Test
    @DisplayName("getPosicionesActuales retorna lista vacia cuando no hay tracks")
    void getPosicionesActuales_sinTracks_retornaListaVacia() {
        when(gpsRepo.findUltimasPosiciones(EMPRESA_ID)).thenReturn(List.of());
        when(vehRepo.findAllById(anyList())).thenReturn(List.of());

        List<GpsPosicionActualDto> resultado = servicio.getPosicionesActuales(EMPRESA_ID);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("getPosicionesActuales no falla cuando el vehiculo no se encuentra en el mapa")
    void getPosicionesActuales_vehiculoNoEncontrado_campoCeroNull() {
        when(gpsRepo.findUltimasPosiciones(EMPRESA_ID)).thenReturn(List.of(trackEjemplo));
        when(vehRepo.findAllById(anyList())).thenReturn(List.of()); // vehiculo no existe

        List<GpsPosicionActualDto> resultado = servicio.getPosicionesActuales(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPatente()).isNull(); // sin vehiculo, patente es null
    }

    // ─── getRecorrido ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRecorrido retorna tracks del vehiculo en el rango de fechas")
    void getRecorrido_conRango_retornaTracks() {
        LocalDateTime desde = LocalDateTime.now().minusHours(2);
        LocalDateTime hasta = LocalDateTime.now();
        when(gpsRepo.findRecorrido(VEHICULO_ID, desde, hasta))
            .thenReturn(List.of(trackEjemplo));

        List<GpsTrack> resultado = servicio.getRecorrido(VEHICULO_ID, desde, hasta);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getVehiculoId()).isEqualTo(VEHICULO_ID);
        verify(gpsRepo).findRecorrido(VEHICULO_ID, desde, hasta);
    }

    // ─── guardarTrackMobile ───────────────────────────────────────────────────

    @Test
    @DisplayName("guardarTrackMobile persiste el track con empresaId del vehiculo")
    void guardarTrackMobile_vehiculoExistente_persisteTrack() {
        when(vehRepo.findById(VEHICULO_ID)).thenReturn(Optional.of(vehiculoEjemplo));
        when(gpsRepo.saveAndFlush(any(GpsTrack.class))).thenAnswer(inv -> {
            GpsTrack t = inv.getArgument(0);
            t.setId(42L);
            return t;
        });

        GpsTrackMobileRequest req = new GpsTrackMobileRequest();
        req.setVehiculoId(VEHICULO_ID);
        req.setLatitud(new BigDecimal("-33.4569"));
        req.setLongitud(new BigDecimal("-70.6483"));
        req.setVelocidad(new BigDecimal("50.0"));

        GpsTrack resultado = servicio.guardarTrackMobile(req);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(resultado.getVehiculoId()).isEqualTo(VEHICULO_ID);
        assertThat(resultado.getId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("guardarTrackMobile lanza 404 cuando el vehiculo no existe")
    void guardarTrackMobile_vehiculoInexistente_lanza404() {
        when(vehRepo.findById("VEH-X")).thenReturn(Optional.empty());

        GpsTrackMobileRequest req = new GpsTrackMobileRequest();
        req.setVehiculoId("VEH-X");
        req.setLatitud(BigDecimal.ZERO);
        req.setLongitud(BigDecimal.ZERO);

        assertThatThrownBy(() -> servicio.guardarTrackMobile(req))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }
}
