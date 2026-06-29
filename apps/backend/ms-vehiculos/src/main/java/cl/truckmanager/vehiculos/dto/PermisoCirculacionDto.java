package cl.truckmanager.vehiculos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PermisoCirculacionDto {
    @NotBlank String vehiculoId;
    String municipalidadId;
    LocalDate fechaPago;
    Long valor;
    LocalDate fechaVencimiento;
    String documento;
}
