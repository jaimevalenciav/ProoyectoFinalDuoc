package cl.truckmanager.taller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrdenTrabajoDto {
    @NotBlank String vehiculoId;
    @NotBlank String tipo;
    @NotBlank String descripcion;
    String mecanicoResponsable;
    LocalDate fechaCierreEst;
    BigDecimal costoManoObra;
    String notas;
    List<TareaOTDto> tareas;
}
