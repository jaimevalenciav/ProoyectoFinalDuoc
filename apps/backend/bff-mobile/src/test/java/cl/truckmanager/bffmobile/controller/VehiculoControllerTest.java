package cl.truckmanager.bffmobile.controller;

import cl.truckmanager.bffmobile.entity.Vehiculo;
import cl.truckmanager.bffmobile.repository.VehiculoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehiculoController — pruebas unitarias")
class VehiculoControllerTest {

    @Mock private VehiculoRepository vehiculoRepository;

    @InjectMocks private VehiculoController controller;

    private Vehiculo vehiculo;

    @BeforeEach
    void setUp() {
        controller = new VehiculoController(vehiculoRepository, new ObjectMapper());

        vehiculo = new Vehiculo();
        vehiculo.setId("VEH-003");
        vehiculo.setPatente("DKNP-57");
        vehiculo.setMarca("SCANIA");
        vehiculo.setModelo("R500 XT");
        vehiculo.setQrCode("QR-DKNP57");
    }

    @Test
    @DisplayName("validarQr con JSON que contiene id valido retorna datos del vehiculo")
    void validarQr_jsonConId_retornaVehiculo() {
        String qrJson = "{\"tipo\":\"vehiculo\",\"id\":\"VEH-003\",\"patente\":\"DKNP-57\"}";
        when(vehiculoRepository.findById("VEH-003")).thenReturn(Optional.of(vehiculo));

        Map<String, Object> resultado = controller.validarQr(qrJson);

        assertThat(resultado.get("vehiculoId")).isEqualTo("VEH-003");
        assertThat(resultado.get("placa")).isEqualTo("DKNP-57");
        assertThat(resultado.get("marca")).isEqualTo("SCANIA");
        assertThat(resultado.get("modelo")).isEqualTo("R500 XT");
        verify(vehiculoRepository).findById("VEH-003");
    }

    @Test
    @DisplayName("validarQr con JSON pero findById vacio cae a findByQrCode")
    void validarQr_jsonConId_findByIdVacio_usaQrCode() {
        String qrJson = "{\"tipo\":\"vehiculo\",\"id\":\"VEH-003\",\"patente\":\"DKNP-57\"}";
        when(vehiculoRepository.findById("VEH-003")).thenReturn(Optional.empty());
        when(vehiculoRepository.findByQrCode(qrJson)).thenReturn(Optional.empty());
        when(vehiculoRepository.findById(qrJson)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.validarQr(qrJson))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("validarQr con string plano busca por QR_CODE")
    void validarQr_stringPlano_buscaPorQrCode() {
        String qrPlano = "QR-DKNP57";
        when(vehiculoRepository.findByQrCode(qrPlano)).thenReturn(Optional.of(vehiculo));

        Map<String, Object> resultado = controller.validarQr(qrPlano);

        assertThat(resultado.get("vehiculoId")).isEqualTo("VEH-003");
        verify(vehiculoRepository).findByQrCode(qrPlano);
        verify(vehiculoRepository, never()).findById("VEH-003");
    }

    @Test
    @DisplayName("validarQr con string plano sin match lanza 404")
    void validarQr_stringPlano_sinMatch_lanza404() {
        String qrPlano = "QR-INVALIDO";
        when(vehiculoRepository.findByQrCode(qrPlano)).thenReturn(Optional.empty());
        when(vehiculoRepository.findById(qrPlano)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.validarQr(qrPlano))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("obtenerVehiculo retorna datos basicos del vehiculo")
    void obtenerVehiculo_retornaDatos() {
        when(vehiculoRepository.findById("VEH-003")).thenReturn(Optional.of(vehiculo));

        Map<String, Object> resultado = controller.obtenerVehiculo("VEH-003");

        assertThat(resultado.get("id")).isEqualTo("VEH-003");
        assertThat(resultado.get("patente")).isEqualTo("DKNP-57");
    }

    @Test
    @DisplayName("obtenerVehiculo lanza 404 si no existe")
    void obtenerVehiculo_lanza404SiNoExiste() {
        when(vehiculoRepository.findById("VEH-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.obtenerVehiculo("VEH-999"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
