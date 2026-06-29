package cl.truckmanager.vehiculos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "REVISIONES_TECNICAS")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RevisionTecnica {

    @Id @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "VEHICULO_ID", length = 36, nullable = false)
    private String vehiculoId;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "PLANTA_ID", length = 36)
    private String plantaId;

    @Column(name = "FECHA_REVISION")
    private LocalDate fechaRevision;

    @Column(name = "VALOR")
    private Long valor;

    @Column(name = "FECHA_VENCIMIENTO")
    private LocalDate fechaVencimiento;

    /** APROBADO | RECHAZADO | CONDICIONADO */
    @Column(name = "RESULTADO", length = 20)
    private String resultado;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    }
}
