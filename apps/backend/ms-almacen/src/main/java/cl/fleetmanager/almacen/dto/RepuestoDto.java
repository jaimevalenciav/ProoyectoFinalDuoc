package cl.fleetmanager.almacen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RepuestoDto {

    @NotBlank
    private String codigo;

    @NotBlank
    private String descripcion;

    private String categoria;

    private String unidad;

    private BigDecimal stockActual;

    private BigDecimal stockMinimo;

    private BigDecimal precioUnitario;

    private String proveedor;
}
