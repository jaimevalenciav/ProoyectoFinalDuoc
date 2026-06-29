package cl.truckmanager.vehiculos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SucursalDto {
    @NotBlank String nombre;
    String direccion;
    String ciudad;
}
