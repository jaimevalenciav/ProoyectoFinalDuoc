package cl.fleetmanager.conductores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ConductorDto {
    @NotBlank String  nombre;
    @NotBlank String  rut;
    String            telefono;
    String            email;
    @NotBlank String  categoriaLicencia;
    @NotNull  LocalDate vencimientoLicencia;
    String            estado;
}
