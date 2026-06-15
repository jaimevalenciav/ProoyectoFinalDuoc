package cl.fleetmanager.conductores.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa una invitación para que un usuario se una a una empresa.
 * El admin genera el token; el nuevo usuario lo ingresa en el onboarding
 * tras registrarse en Azure B2C.
 */
@Entity
@Table(name = "INVITACIONES_PENDIENTES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitacionPendiente {

    @Id
    @Column(name = "TOKEN", length = 36)
    private String token;

    /** Empresa a la que se unirá el usuario */
    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    /** Rol que se asignará al usuario invitado */
    @Column(name = "ROL", length = 30, nullable = false)
    private String rol;

    /** Email sugerido (puede diferir si el invitado usa otro) */
    @Column(name = "EMAIL_SUGERIDO", length = 200)
    private String emailSugerido;

    /** Nota descriptiva del admin (ej: "Mecánico turno noche") */
    @Column(name = "NOTA", length = 300)
    private String nota;

    /** Cuándo expira la invitación */
    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    /** Si ya fue utilizada */
    @Column(name = "USADA")
    @Builder.Default
    private Boolean usada = false;

    /** AzureOid del usuario que aceptó (para auditoría) */
    @Column(name = "ACEPTADA_POR_OID", length = 100)
    private String aceptadaPorOid;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (token == null || token.isBlank()) token = UUID.randomUUID().toString();
        if (usada == null) usada = false;
        createdAt = LocalDateTime.now();
        if (expiresAt == null) expiresAt = LocalDateTime.now().plusDays(7);
    }
}
