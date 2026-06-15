package cl.fleetmanager.operaciones.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CargaCombustibleDto {
    private String vehiculoId;
    private String conductorId;
    private String numDocumento;
    private String proveedor;
    private String estacion;
    private LocalDate fechaCarga;
    private BigDecimal litros;
    private BigDecimal precioLitro;
    private Long kmVehiculo;
    private String tipoCombustible;
}
