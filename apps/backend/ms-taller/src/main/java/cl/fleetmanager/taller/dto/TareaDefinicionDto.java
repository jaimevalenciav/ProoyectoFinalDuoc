package cl.fleetmanager.taller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TareaDefinicionDto {

    @NotBlank
    private String nombre;

    private String descripcion;

    private String tipoOt;

    private List<TareaDefArticuloDto> articulos = new ArrayList<>();

    @Data
    public static class TareaDefArticuloDto {

        @NotBlank
        private String repuestoId;

        private BigDecimal cantidad;
    }
}
