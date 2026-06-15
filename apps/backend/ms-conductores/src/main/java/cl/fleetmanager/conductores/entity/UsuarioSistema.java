package cl.fleetmanager.conductores.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "USUARIOS_SISTEMA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioSistema {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "AZURE_OID", length = 100)
    private String azureOid;

    @Column(name = "NOMBRE", length = 200)
    private String nombre;

    @Column(name = "EMAIL", length = 200)
    private String email;

    /** ADMIN | SUPERVISOR_TALLER | MECANICO_TALLER | COMERCIAL | CONTABILIDAD */
    @Column(name = "ROL", length = 30)
    private String rol;

    @Column(name = "ACTIVO")
    private Integer activo;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (activo == null)  activo  = 1;
        if (rol == null)     rol     = "ADMIN";
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
