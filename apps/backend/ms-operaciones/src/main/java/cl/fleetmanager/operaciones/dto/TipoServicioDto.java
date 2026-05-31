package cl.fleetmanager.operaciones.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TipoServicioDto {
    @NotBlank String codigo;
    @NotBlank String nombre;
    Integer activo;
}
