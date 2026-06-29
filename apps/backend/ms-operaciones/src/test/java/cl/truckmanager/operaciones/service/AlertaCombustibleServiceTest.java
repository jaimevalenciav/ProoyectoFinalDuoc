package cl.truckmanager.operaciones.service;

import cl.truckmanager.operaciones.dto.AlertaCombustibleDto;
import cl.truckmanager.operaciones.entity.AlertaCombustible;
import cl.truckmanager.operaciones.repository.AlertaCombustibleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertaCombustibleService — pruebas unitarias")
class AlertaCombustibleServiceTest {

    @Mock
    private AlertaCombustibleRepository repo;

    @InjectMocks
    private AlertaCombustibleService servicio;

    private static final String EMPRESA_ID  = "EMP-001";
    private static final String ALERTA_ID   = "ALE-001";

    private AlertaCombustible alertaEjemplo;

    @BeforeEach
    void configurar() {
        alertaEjemplo = new AlertaCombustible();
        alertaEjemplo.setId(ALERTA_ID);
        alertaEjemplo.setEmpresaId(EMPRESA_ID);
        alertaEjemplo.setCargaId("CAR-001");
        alertaEjemplo.setVehiculoId("VEH-001");
        alertaEjemplo.setTipo("warning");
        alertaEjemplo.setMensaje("Consumo anomalo alto: 42.0 L/100km");
        alertaEjemplo.setLeida(0);
    }

    // ─── guardar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar persiste alertas de tipo warning e info, omite error")
    void guardar_alertasMixtas_persisteSoloWarningInfo() {
        AlertaCombustibleDto dtoWarning = new AlertaCombustibleDto();
        dtoWarning.setCargaId("CAR-001");
        dtoWarning.setVehiculoId("VEH-001");
        dtoWarning.setTipo("warning");
        dtoWarning.setMensaje("Consumo alto");

        AlertaCombustibleDto dtoError = new AlertaCombustibleDto();
        dtoError.setCargaId("CAR-001");
        dtoError.setVehiculoId("VEH-001");
        dtoError.setTipo("error");
        dtoError.setMensaje("Error critico");

        AlertaCombustibleDto dtoInfo = new AlertaCombustibleDto();
        dtoInfo.setCargaId("CAR-001");
        dtoInfo.setVehiculoId("VEH-001");
        dtoInfo.setTipo("info");
        dtoInfo.setMensaje("Informacion");

        servicio.guardar(EMPRESA_ID, List.of(dtoWarning, dtoError, dtoInfo));

        verify(repo, times(2)).save(any(AlertaCombustible.class)); // warning + info
    }

    @Test
    @DisplayName("guardar no persiste nada si todas las alertas son de tipo error")
    void guardar_soloErrores_noGuardaNada() {
        AlertaCombustibleDto dto = new AlertaCombustibleDto();
        dto.setTipo("error");
        dto.setMensaje("Error");
        dto.setCargaId("CAR-001");
        dto.setVehiculoId("VEH-001");

        servicio.guardar(EMPRESA_ID, List.of(dto));

        verify(repo, never()).save(any());
    }

    // ─── getAlertas ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAlertas con activas=true retorna solo las no leidas")
    void getAlertas_activasTrue_retornaNoLeidas() {
        when(repo.findByEmpresaIdAndLeidaOrderByCreatedAtDesc(EMPRESA_ID, 0))
            .thenReturn(List.of(alertaEjemplo));

        List<AlertaCombustible> resultado = servicio.getAlertas(EMPRESA_ID, true);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getLeida()).isEqualTo(0);
    }

    @Test
    @DisplayName("getAlertas con activas=false retorna las ya leidas")
    void getAlertas_activasFalse_retornaLeidas() {
        when(repo.findByEmpresaIdAndLeidaOrderByCreatedAtDesc(EMPRESA_ID, 1))
            .thenReturn(List.of());

        List<AlertaCombustible> resultado = servicio.getAlertas(EMPRESA_ID, false);

        assertThat(resultado).isEmpty();
        verify(repo).findByEmpresaIdAndLeidaOrderByCreatedAtDesc(EMPRESA_ID, 1);
    }

    @Test
    @DisplayName("getAlertas con activas=null retorna todas sin filtrar")
    void getAlertas_activasNull_retornaTodasSinFiltrar() {
        when(repo.findByEmpresaIdOrderByCreatedAtDesc(EMPRESA_ID))
            .thenReturn(List.of(alertaEjemplo));

        List<AlertaCombustible> resultado = servicio.getAlertas(EMPRESA_ID, null);

        assertThat(resultado).hasSize(1);
        verify(repo).findByEmpresaIdOrderByCreatedAtDesc(EMPRESA_ID);
    }

    // ─── marcarLeida ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("marcarLeida actualiza leida=1 y registra quien la confirmo")
    void marcarLeida_alertaExistente_actualizaLeida() {
        when(repo.findById(ALERTA_ID)).thenReturn(Optional.of(alertaEjemplo));
        when(repo.save(any(AlertaCombustible.class))).thenAnswer(inv -> inv.getArgument(0));

        AlertaCombustible resultado = servicio.marcarLeida(ALERTA_ID, "USR-001");

        assertThat(resultado.getLeida()).isEqualTo(1);
        assertThat(resultado.getLeidaPor()).isEqualTo("USR-001");
        assertThat(resultado.getLeidaAt()).isNotNull();
    }

    @Test
    @DisplayName("marcarLeida usa Sistema cuando leidaPor es null o en blanco")
    void marcarLeida_sinLeidaPor_usaSistema() {
        when(repo.findById(ALERTA_ID)).thenReturn(Optional.of(alertaEjemplo));
        when(repo.save(any(AlertaCombustible.class))).thenAnswer(inv -> inv.getArgument(0));

        AlertaCombustible resultado = servicio.marcarLeida(ALERTA_ID, null);

        assertThat(resultado.getLeidaPor()).isEqualTo("Sistema");
    }

    @Test
    @DisplayName("marcarLeida lanza EntityNotFoundException cuando no existe")
    void marcarLeida_inexistente_lanzaExcepcion() {
        when(repo.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.marcarLeida("X", "USR-001"))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
