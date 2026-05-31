package cl.fleetmanager.taller.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "TAREAS_DEFINICION")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "articulos")
@ToString(exclude = "articulos")
public class TareaDefinicion {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "NOMBRE", length = 200, nullable = false)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;

    @Column(name = "TIPO_OT", length = 20)
    private String tipoOt;

    @Column(name = "ACTIVO")
    private Integer activo;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @JsonManagedReference
    @OneToMany(mappedBy = "tareaDefinicion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<TareaDefArticulo> articulos = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (activo == null) activo = 1;
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
