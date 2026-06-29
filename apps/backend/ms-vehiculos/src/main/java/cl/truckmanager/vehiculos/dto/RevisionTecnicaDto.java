package cl.truckmanager.vehiculos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RevisionTecnicaDto {
    @NotBlank String vehiculoId;
    String plantaId;
    LocalDate fechaRevision;
    Long valor;
    LocalDate fechaVencimiento;
    /** APROBADO | RECHAZADO | CONDICIONADO */
    String resultado;
}
