package cl.truckmanager.conductores.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Payload para crear o actualizar un usuario del sistema */
@Data
public class UsuarioDto {

    @NotBlank
    private String nombre;

    @Email
    @NotBlank
    private String email;

    /** ADMIN | SUPERVISOR_TALLER | MECANICO_TALLER | COMERCIAL | CONTABILIDAD */
    @NotBlank
    private String rol;

    /** OID de Azure AD B2C (opcional al crear, se vincula después del primer login) */
    private String azureOid;

    private Integer activo;
}
