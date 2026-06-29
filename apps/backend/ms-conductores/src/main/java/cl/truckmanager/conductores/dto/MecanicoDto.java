package cl.truckmanager.conductores.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MecanicoDto {
    @NotBlank
    private String nombre;
    private String rut;
    private String especialidad;
    private String telefono;
    private String email;
    private Integer activo;
    private String observacion;
}
