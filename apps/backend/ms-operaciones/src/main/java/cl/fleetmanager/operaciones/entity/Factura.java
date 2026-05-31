package cl.fleetmanager.operaciones.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "FACTURAS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "CLIENTE_ID", length = 36, nullable = false)
    private String clienteId;

    @Column(name = "NUM_FACTURA", length = 20, nullable = false)
    private String numFactura;

    @Column(name = "FECHA_EMISION")
    private LocalDate fechaEmision;

    @Column(name = "SUBTOTAL", precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "IVA", precision = 14, scale = 2)
    private BigDecimal iva;

    @Column(name = "TOTAL", precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "ESTADO", length = 20)
    private String estado;

    @Column(name = "NOTAS", length = 1000)
    private String notas;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Transient
    private String clienteRazonSocial;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (estado == null) estado = "EMITIDA";
        if (fechaEmision == null) fechaEmision = LocalDate.now();
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        if (iva      == null) iva      = BigDecimal.ZERO;
        if (total    == null) total    = BigDecimal.ZERO;
        createdAt = LocalDateTime.now();
    }
}
