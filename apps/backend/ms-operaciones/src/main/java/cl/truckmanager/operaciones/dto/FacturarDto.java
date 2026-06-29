package cl.truckmanager.operaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class FacturarDto {
    @NotBlank String clienteId;
    @NotEmpty List<String> servicioIds;
    String notas;
}
