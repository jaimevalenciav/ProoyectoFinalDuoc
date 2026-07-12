package cl.truckmanager.bffmobile.service;

import cl.truckmanager.bffmobile.dto.GpsTrackRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GpsPublisherService — pruebas unitarias")
class GpsPublisherServiceTest {

    @Mock
    private ObjectMapper mapeadorJson;

    @InjectMocks
    private GpsPublisherService servicio;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(servicio, "cadenaConexion", "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=dGVzdA==");
        ReflectionTestUtils.setField(servicio, "nombreCola", "pistas-gps");
    }

    @Test
    @DisplayName("publicar no lanza excepcion aunque Service Bus no este disponible")
    void publicar_noLanzaExcepcion_cuandoServiceBusFalla() throws Exception {
        GpsTrackRequest solicitud = new GpsTrackRequest();
        solicitud.setIdVehiculo("VEH-003");
        solicitud.setIdConductor("CON-001");
        solicitud.setLatitud(-34.436942);
        solicitud.setLongitud(-71.094371);

        when(mapeadorJson.writeValueAsString(solicitud)).thenReturn("{\"idVehiculo\":\"VEH-003\"}");

        assertThatNoException().isThrownBy(() -> servicio.publicar(solicitud));
    }

    @Test
    @DisplayName("publicar maneja excepcion de serializacion sin propagar")
    void publicar_manejaExcepcionSerializacion() throws Exception {
        GpsTrackRequest solicitud = new GpsTrackRequest();
        solicitud.setIdVehiculo("VEH-001");

        when(mapeadorJson.writeValueAsString(solicitud))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Error serializing") {});

        assertThatNoException().isThrownBy(() -> servicio.publicar(solicitud));
    }
}
