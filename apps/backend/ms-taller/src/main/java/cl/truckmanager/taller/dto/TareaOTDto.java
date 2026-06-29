package cl.truckmanager.taller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TareaOTDto {
    @NotBlank String descripcion;
    Integer orden;
}
