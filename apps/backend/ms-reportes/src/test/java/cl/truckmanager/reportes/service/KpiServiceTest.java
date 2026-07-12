package cl.truckmanager.reportes.service;

import cl.truckmanager.reportes.dto.KpiDashboardDto;
import cl.truckmanager.reportes.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KpiService — pruebas unitarias")
class KpiServiceTest {

    @Mock private VehiculoKpiRepository vehiculoRepo;
    @Mock private OtKpiRepository       otRepo;
    @Mock private ConductorKpiRepository conductorRepo;
    @Mock private RepuestoKpiRepository  repuestoRepo;

    @InjectMocks private KpiService servicio;

    private static final String EMP = "EMP-001";

    @Test
    @DisplayName("getDashboard retorna KPI con todos los conteos correctos")
    void getDashboard_retornaKpiCompleto() {
        when(vehiculoRepo.countVehiculos(EMP)).thenReturn(10L);
        when(vehiculoRepo.countOperativos(EMP)).thenReturn(7L);
        when(vehiculoRepo.countEnTaller(EMP)).thenReturn(2L);
        when(otRepo.countOts(EMP)).thenReturn(15L);
        when(otRepo.countOtsPendientes(EMP)).thenReturn(5L);
        when(otRepo.countOtsEnEjecucion(EMP)).thenReturn(4L);
        when(otRepo.countOtsCerradas(EMP)).thenReturn(6L);
        when(conductorRepo.countConductores(EMP)).thenReturn(8L);
        when(repuestoRepo.countBajoStock(EMP)).thenReturn(3L);

        KpiDashboardDto resultado = servicio.getDashboard(EMP);

        assertThat(resultado.getTotalVehiculos()).isEqualTo(10L);
        assertThat(resultado.getVehiculosOperativos()).isEqualTo(7L);
        assertThat(resultado.getVehiculosEnTaller()).isEqualTo(2L);
        assertThat(resultado.getVehiculosFuera()).isEqualTo(1L);
        assertThat(resultado.getTotalOts()).isEqualTo(15L);
        assertThat(resultado.getOtsPendientes()).isEqualTo(5L);
        assertThat(resultado.getOtsEnEjecucion()).isEqualTo(4L);
        assertThat(resultado.getOtsCerradas()).isEqualTo(6L);
        assertThat(resultado.getTotalConductores()).isEqualTo(8L);
        assertThat(resultado.getAlertasBajoStock()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getDashboard vehiculosFuera no puede ser negativo")
    void getDashboard_vehiculosFueraNoNegativo() {
        when(vehiculoRepo.countVehiculos(EMP)).thenReturn(5L);
        when(vehiculoRepo.countOperativos(EMP)).thenReturn(4L);
        when(vehiculoRepo.countEnTaller(EMP)).thenReturn(3L);
        when(otRepo.countOts(EMP)).thenReturn(0L);
        when(otRepo.countOtsPendientes(EMP)).thenReturn(0L);
        when(otRepo.countOtsEnEjecucion(EMP)).thenReturn(0L);
        when(otRepo.countOtsCerradas(EMP)).thenReturn(0L);
        when(conductorRepo.countConductores(EMP)).thenReturn(0L);
        when(repuestoRepo.countBajoStock(EMP)).thenReturn(0L);

        KpiDashboardDto resultado = servicio.getDashboard(EMP);

        assertThat(resultado.getVehiculosFuera()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("getDashboard con empresa sin datos retorna conteos en cero")
    void getDashboard_sinDatos_retornaCeros() {
        when(vehiculoRepo.countVehiculos(EMP)).thenReturn(0L);
        when(vehiculoRepo.countOperativos(EMP)).thenReturn(0L);
        when(vehiculoRepo.countEnTaller(EMP)).thenReturn(0L);
        when(otRepo.countOts(EMP)).thenReturn(0L);
        when(otRepo.countOtsPendientes(EMP)).thenReturn(0L);
        when(otRepo.countOtsEnEjecucion(EMP)).thenReturn(0L);
        when(otRepo.countOtsCerradas(EMP)).thenReturn(0L);
        when(conductorRepo.countConductores(EMP)).thenReturn(0L);
        when(repuestoRepo.countBajoStock(EMP)).thenReturn(0L);

        KpiDashboardDto resultado = servicio.getDashboard(EMP);

        assertThat(resultado.getTotalVehiculos()).isZero();
        assertThat(resultado.getTotalOts()).isZero();
        assertThat(resultado.getTotalConductores()).isZero();
        assertThat(resultado.getAlertasBajoStock()).isZero();
    }
}
