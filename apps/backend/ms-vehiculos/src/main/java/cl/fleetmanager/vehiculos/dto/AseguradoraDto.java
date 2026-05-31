package cl.fleetmanager.vehiculos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AseguradoraDto {
    @NotBlank String nombre;
    String rut;
}
