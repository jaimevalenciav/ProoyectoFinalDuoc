package cl.truckmanager.reportes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostoMantenimientoDto {

    /** Formato "YYYY-MM" */
    private String mes;
    private BigDecimal costoManoObra;
    private BigDecimal costoRepuestos;
    private BigDecimal total;
}
