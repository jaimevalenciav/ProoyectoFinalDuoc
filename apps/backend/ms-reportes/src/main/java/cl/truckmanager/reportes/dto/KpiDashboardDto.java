package cl.truckmanager.reportes.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KpiDashboardDto {

    private long totalVehiculos;
    private long vehiculosOperativos;
    private long vehiculosEnTaller;
    private long vehiculosFuera;

    private long totalOts;
    private long otsPendientes;
    private long otsEnEjecucion;
    private long otsCerradas;

    private long totalConductores;

    private long alertasBajoStock;
}
