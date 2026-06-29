package cl.truckmanager.vehiculos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MunicipalidadDto {
    @NotBlank String nombre;
    String region;
}
