package cl.truckmanager.taller.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "TAREAS_OT")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "ordenTrabajo")
@ToString(exclude = "ordenTrabajo")
public class TareaOT {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OT_ID", nullable = false)
    private OrdenTrabajo ordenTrabajo;

    @Column(name = "DESCRIPCION", length = 300, nullable = false)
    private String descripcion;

    @Column(name = "COMPLETADA")
    private Integer completada;

    @Column(name = "ORDEN")
    private Integer orden;

    @Column(name = "COMPLETADA_AT")
    private LocalDateTime completadaAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (completada == null) completada = 0;
        if (orden == null)      orden      = 0;
        createdAt = LocalDateTime.now();
    }
}
