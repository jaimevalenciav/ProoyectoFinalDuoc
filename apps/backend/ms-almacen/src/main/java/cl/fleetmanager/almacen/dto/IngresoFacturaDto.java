package cl.fleetmanager.almacen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class IngresoFacturaDto {

    @NotBlank
    private String tipoDocumento;   // FACTURA | GUIA_DESPACHO

    @NotBlank
    private String numDocumento;

    @NotBlank
    private String proveedor;

    private LocalDate fechaDocumento;

    @NotEmpty
    private List<LineaDto> lineas;

    @Data
    public static class LineaDto {
        @NotBlank
        private String repuestoId;
        private BigDecimal cantidad;
        private BigDecimal precioUnit;
    }
}
