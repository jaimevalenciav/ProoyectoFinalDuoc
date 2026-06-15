package cl.fleetmanager.conductores.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Payload para crear una invitación */
@Data
public class InvitacionDto {

    /** Rol que se asignará al invitado */
    @NotBlank
    private String rol;

    /** Email del invitado (opcional, informativo) */
    private String emailSugerido;

    /** Nota descriptiva (ej: "Mecánico turno noche") */
    private String nota;

    /** Días de vigencia (default 7) */
    private Integer diasVigencia;
}
