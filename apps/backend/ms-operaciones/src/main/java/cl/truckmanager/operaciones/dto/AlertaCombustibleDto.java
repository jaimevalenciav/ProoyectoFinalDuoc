package cl.truckmanager.operaciones.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AlertaCombustibleDto {
    @NotBlank private String cargaId;
    @NotBlank private String vehiculoId;
    @NotBlank private String tipo;      // 'error' | 'warning' | 'info'
    private String icono;
    @NotBlank private String mensaje;
}
