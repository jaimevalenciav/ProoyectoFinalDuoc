package cl.truckmanager.conductores.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "MECANICOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mecanico {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "NOMBRE", length = 200, nullable = false)
    private String nombre;

    @Column(name = "RUT", length = 20)
    private String rut;

    /** MECANICO_GENERAL | ELECTRICO | NEUMATICOS | CARROCERIA | HIDRAULICA */
    @Column(name = "ESPECIALIDAD", length = 40)
    private String especialidad;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "EMAIL", length = 150)
    private String email;

    /** 1 = activo, 0 = inactivo */
    @Column(name = "ACTIVO")
    private Integer activo;

    @Column(name = "OBSERVACION", length = 500)
    private String observacion;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (activo == null) activo = 1;
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
