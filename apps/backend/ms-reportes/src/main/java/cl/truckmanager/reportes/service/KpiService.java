package cl.truckmanager.reportes.service;

import cl.truckmanager.reportes.dto.KpiDashboardDto;
import cl.truckmanager.reportes.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KpiService {

    private final VehiculoKpiRepository vehiculoRepo;
    private final OtKpiRepository otRepo;
    private final ConductorKpiRepository conductorRepo;
    private final RepuestoKpiRepository repuestoRepo;
    private final CargaCombustibleKpiRepository combustibleRepo;
    private final ServicioKpiRepository servicioRepo;

    public KpiDashboardDto getDashboard(String empresaId, LocalDate desde, LocalDate hasta) {
        long totalVehiculos = vehiculoRepo.countVehiculos(empresaId);
        long operativos     = vehiculoRepo.countOperativos(empresaId);
        long enTaller       = vehiculoRepo.countEnTaller(empresaId);
        long fuera          = Math.max(0, totalVehiculos - operativos - enTaller);

        long totalOts       = otRepo.countOts(empresaId);
        long pendientes     = otRepo.countOtsPendientes(empresaId);
        long enEjecucion    = otRepo.countOtsEnEjecucion(empresaId);
        long cerradas       = otRepo.countOtsCerradas(empresaId);

        long conductores    = conductorRepo.countConductores(empresaId);
        long bajoStock      = repuestoRepo.countBajoStock(empresaId);

        // KPIs de período
        long   otsCerradasMes     = otRepo.countOtsCerradasPeriodo(empresaId, desde, hasta);
        double costoMantPeriodo   = otRepo.sumCostoMantenimientoPeriodo(empresaId, desde, hasta);
        double litrosPeriodo      = combustibleRepo.sumLitrosPeriodo(empresaId, desde, hasta);
        double costoCombPeriodo   = combustibleRepo.sumCostoCombustiblePeriodo(empresaId, desde, hasta);
        double ingresosPeriodo    = servicioRepo.sumIngresosPeriodo(empresaId, desde, hasta);
        double kmPeriodo          = servicioRepo.sumKmRecorridosPeriodo(empresaId, desde, hasta);

        return KpiDashboardDto.builder()
                .vehiculosActivos(operativos)
                .vehiculosEnTaller(enTaller)
                .vehiculosFuera(fuera)
                .totalVehiculos(totalVehiculos)
                .totalOts(totalOts)
                .otsPendientes(pendientes)
                .otsEnEjecucion(enEjecucion)
                .otCerradasMes(otsCerradasMes)
                .totalConductores(conductores)
                .alertasBajoStock(bajoStock)
                .litrosCargadosMes(litrosPeriodo)
                .costoCombustibleMes(costoCombPeriodo)
                .costoMantenimientoMes(costoMantPeriodo)
                .ingresoServiciosMes(ingresosPeriodo)
                .kmRecorridosMes(kmPeriodo)
                .build();
    }
}
