package cl.fleetmanager.conductores.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Vista de una invitación para mostrar en la UI */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitacionResumenDto {
    private String        token;
    private String        rol;
    private String        emailSugerido;
    private String        nota;
    private LocalDateTime expiresAt;
    private Boolean       usada;
    private LocalDateTime createdAt;
}
