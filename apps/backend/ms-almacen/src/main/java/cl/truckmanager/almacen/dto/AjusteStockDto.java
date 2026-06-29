package cl.truckmanager.almacen.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AjusteStockDto {

    /** ENTRADA, SALIDA o AJUSTE */
    private String tipo;

    private BigDecimal cantidad;

    /** Opcional: ID de la OT relacionada */
    private String otId;

    private String referencia;

    private String documento;

    private String observacion;
}
