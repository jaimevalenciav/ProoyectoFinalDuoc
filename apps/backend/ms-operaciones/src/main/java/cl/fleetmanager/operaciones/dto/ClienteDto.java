package cl.fleetmanager.operaciones.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteDto {
    @NotBlank String rut;
    @NotBlank String razonSocial;
    String giro;
    String ciudad;
    String telefono;
    String email;
    String repLegalNombre;
    String repLegalRut;
    Integer activo;
}
