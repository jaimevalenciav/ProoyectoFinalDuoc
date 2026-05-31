package cl.fleetmanager.vehiculos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlantaRevisionDto {
    @NotBlank String nombre;
    String direccion;
}
