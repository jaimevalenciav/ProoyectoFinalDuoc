package cl.fleetmanager.almacen.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "REPUESTOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repuesto {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "CODIGO", length = 50, nullable = false)
    private String codigo;

    @Column(name = "DESCRIPCION", length = 300, nullable = false)
    private String descripcion;

    @Column(name = "CATEGORIA", length = 100)
    private String categoria;

    @Column(name = "UNIDAD", length = 20)
    private String unidad;

    @Column(name = "STOCK_ACTUAL", precision = 10, scale = 2)
    private BigDecimal stockActual;

    @Column(name = "STOCK_MINIMO", precision = 10, scale = 2)
    private BigDecimal stockMinimo;

    @Column(name = "PRECIO_UNITARIO", precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "PROVEEDOR", length = 200)
    private String proveedor;

    @Column(name = "ELIMINADO")
    private Integer eliminado;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (eliminado == null)    eliminado    = 0;
        if (stockActual == null)  stockActual  = BigDecimal.ZERO;
        if (stockMinimo == null)  stockMinimo  = BigDecimal.ONE;
        if (unidad == null)       unidad       = "UN";
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
