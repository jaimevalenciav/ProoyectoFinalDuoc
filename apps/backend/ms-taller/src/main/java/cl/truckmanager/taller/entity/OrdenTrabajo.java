package cl.truckmanager.taller.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "ORDENES_TRABAJO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenTrabajo {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "NUMERO", length = 20, nullable = false)
    private String numero;

    @Column(name = "VEHICULO_ID", length = 36, nullable = false)
    private String vehiculoId;

    @Column(name = "TIPO", length = 20, nullable = false)
    private String tipo;

    @Column(name = "ESTADO", length = 20, nullable = false)
    private String estado;

    @Column(name = "DESCRIPCION", length = 1000, nullable = false)
    private String descripcion;

    @Column(name = "MECANICO_RESPONSABLE", length = 200)
    private String mecanicoResponsable;

    @Column(name = "AVANCE")
    private Integer avance;

    @Column(name = "COSTO_MANO_OBRA", precision = 14, scale = 2)
    private BigDecimal costoManoObra;

    @Column(name = "COSTO_REPUESTOS", precision = 14, scale = 2)
    private BigDecimal costoRepuestos;

    @Column(name = "COSTO_TOTAL", precision = 14, scale = 2)
    private BigDecimal costoTotal;

    @Column(name = "FECHA_APERTURA")
    private LocalDate fechaApertura;

    @Column(name = "FECHA_CIERRE_EST")
    private LocalDate fechaCierreEst;

    @Column(name = "FECHA_CIERRE_REAL")
    private LocalDate fechaCierreReal;

    @Column(name = "NOTAS", length = 2000)
    private String notas;

    @Column(name = "ELIMINADO")
    private Integer eliminado;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @JsonManagedReference
    @OneToMany(mappedBy = "ordenTrabajo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<TareaOT> tareas = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (estado == null)       estado       = "PENDIENTE";
        if (avance == null)       avance       = 0;
        if (eliminado == null)    eliminado    = 0;
        if (costoManoObra == null) costoManoObra = BigDecimal.ZERO;
        if (costoRepuestos == null) costoRepuestos = BigDecimal.ZERO;
        if (costoTotal == null)   costoTotal   = BigDecimal.ZERO;
        if (fechaApertura == null) fechaApertura = LocalDate.now();
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
