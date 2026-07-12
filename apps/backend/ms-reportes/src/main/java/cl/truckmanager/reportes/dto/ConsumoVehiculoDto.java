package cl.truckmanager.reportes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumoVehiculoDto {

    private String vehiculoId;
    private String placa;
    private BigDecimal litrosTotales;
    private Long kmTotales;
    private BigDecimal rendimientoPromedio; // km por litro
    private BigDecimal costoTotal;
}
