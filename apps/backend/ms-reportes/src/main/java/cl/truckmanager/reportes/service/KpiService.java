package cl.truckmanager.reportes.service;

import cl.truckmanager.reportes.dto.KpiDashboardDto;
import cl.truckmanager.reportes.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KpiService {

    private final VehiculoKpiRepository vehiculoRepo;
    private final OtKpiRepository otRepo;
    private final ConductorKpiRepository conductorRepo;
    private final RepuestoKpiRepository repuestoRepo;

    public KpiDashboardDto getDashboard(String empresaId) {
        long totalVehiculos     = vehiculoRepo.countVehiculos(empresaId);
        long operativos         = vehiculoRepo.countOperativos(empresaId);
        long enTaller           = vehiculoRepo.countEnTaller(empresaId);
        long fuera              = totalVehiculos - operativos - enTaller;

        long totalOts           = otRepo.countOts(empresaId);
        long pendientes         = otRepo.countOtsPendientes(empresaId);
        long enEjecucion        = otRepo.countOtsEnEjecucion(empresaId);
        long cerradas           = otRepo.countOtsCerradas(empresaId);

        long conductores        = conductorRepo.countConductores(empresaId);
        long bajoStock          = repuestoRepo.countBajoStock(empresaId);

        return KpiDashboardDto.builder()
                .totalVehiculos(totalVehiculos)
                .vehiculosOperativos(operativos)
                .vehiculosEnTaller(enTaller)
                .vehiculosFuera(fuera < 0 ? 0 : fuera)
                .totalOts(totalOts)
                .otsPendientes(pendientes)
                .otsEnEjecucion(enEjecucion)
                .otsCerradas(cerradas)
                .totalConductores(conductores)
                .alertasBajoStock(bajoStock)
                .build();
    }
}
