package cl.fleetmanager.taller.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "TAREAS_DEF_ARTICULOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "tareaDefinicion")
@ToString(exclude = "tareaDefinicion")
public class TareaDefArticulo {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TAREA_DEF_ID", nullable = false)
    private TareaDefinicion tareaDefinicion;

    @Column(name = "REPUESTO_ID", length = 36, nullable = false)
    private String repuestoId;

    @Column(name = "CANTIDAD", precision = 10, scale = 2, nullable = false)
    private BigDecimal cantidad;

    /** Nombre del repuesto para mostrar en frontend — no persiste en DB */
    @Transient
    private String repuestoNombre;

    /** Unidad del repuesto para mostrar en frontend — no persiste en DB */
    @Transient
    private String repuestoUnidad;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    }
}
