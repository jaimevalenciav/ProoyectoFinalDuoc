package cl.truckmanager.operaciones.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "TIPOS_SERVICIO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoServicio {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "CODIGO", length = 30, nullable = false)
    private String codigo;

    @Column(name = "NOMBRE", length = 200, nullable = false)
    private String nombre;

    @Column(name = "ACTIVO")
    private Integer activo;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (activo == null) activo = 1;
    }
}
