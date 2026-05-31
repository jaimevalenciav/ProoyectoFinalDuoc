package cl.fleetmanager.almacen.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "MOVIMIENTOS_STOCK")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "repuesto")
@ToString(exclude = "repuesto")
public class MovimientoStock {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPUESTO_ID", nullable = false)
    private Repuesto repuesto;

    @Column(name = "TIPO", length = 20, nullable = false)
    private String tipo;

    @Column(name = "CANTIDAD", precision = 10, scale = 2, nullable = false)
    private BigDecimal cantidad;

    @Column(name = "PRECIO_UNIT", precision = 14, scale = 2)
    private BigDecimal precioUnit;

    @Column(name = "STOCK_ANTERIOR", precision = 10, scale = 2)
    private BigDecimal stockAnterior;

    @Column(name = "STOCK_NUEVO", precision = 10, scale = 2)
    private BigDecimal stockNuevo;

    @Column(name = "COSTO_TOTAL", precision = 14, scale = 2)
    private BigDecimal costoTotal;

    @Column(name = "REFERENCIA", length = 200)
    private String referencia;

    @Column(name = "DOCUMENTO", length = 100)
    private String documento;

    @Column(name = "OT_ID", length = 36)
    private String otId;

    @Column(name = "USUARIO_ID", length = 36)
    private String usuarioId;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }
}
