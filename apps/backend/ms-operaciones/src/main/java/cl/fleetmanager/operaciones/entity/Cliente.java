package cl.fleetmanager.operaciones.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tabla CLIENTES — gestionada íntegramente por ms-operaciones.
 * Se permite lectura y escritura completa desde este microservicio.
 */
@Entity
@Table(name = "CLIENTES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "RUT", length = 20, nullable = false)
    private String rut;

    @Column(name = "RAZON_SOCIAL", length = 200, nullable = false)
    private String razonSocial;

    @Column(name = "GIRO", length = 200)
    private String giro;

    @Column(name = "CIUDAD", length = 100)
    private String ciudad;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "EMAIL", length = 150)
    private String email;

    @Column(name = "REP_LEGAL_NOMBRE", length = 200)
    private String repLegalNombre;

    @Column(name = "REP_LEGAL_RUT", length = 20)
    private String repLegalRut;

    @Column(name = "ACTIVO")
    private Integer activo;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (activo == null) activo = 1;
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
