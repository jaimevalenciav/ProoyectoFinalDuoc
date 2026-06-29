package cl.truckmanager.vehiculos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class SeguroSoapDto {
    @NotBlank String vehiculoId;
    String aseguradoraId;
    LocalDate fechaEmision;
    Long valor;
    LocalDate fechaVencimiento;
    String poliza;
}
