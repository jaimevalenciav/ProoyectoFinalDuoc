package cl.truckmanager.reportes.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KpiDashboardDto {

    // Nombres que espera el frontend Angular
    private long vehiculosActivos;
    private long vehiculosEnTaller;
    private long vehiculosFuera;
    private long totalVehiculos;

    private long totalOts;
    private long otsPendientes;
    private long otsEnEjecucion;
    private long otCerradasMes;

    private long totalConductores;
    private long alertasBajoStock;

    // KPIs de período (dependen de desde/hasta)
    private double litrosCargadosMes;
    private double costoCombustibleMes;
    private double costoMantenimientoMes;
    private double ingresoServiciosMes;
    private double kmRecorridosMes;
}
