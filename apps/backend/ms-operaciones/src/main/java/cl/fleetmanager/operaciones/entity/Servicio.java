package cl.fleetmanager.operaciones.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "SERVICIOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "CLIENTE_ID", length = 36)
    private String clienteId;

    @Column(name = "VEHICULO_ID", length = 36)
    private String vehiculoId;

    @Column(name = "CONDUCTOR_ID", length = 36)
    private String conductorId;

    @Column(name = "NUM_SERVICIO", length = 20)
    private String numServicio;

    @Column(name = "ORIGEN", length = 200)
    private String origen;

    @Column(name = "DESTINO", length = 200)
    private String destino;

    @Column(name = "KMS_RECORRIDO", precision = 10, scale = 2)
    private BigDecimal kmsRecorrido;

    @Column(name = "FECHA_SERVICIO")
    private LocalDate fechaServicio;

    @Column(name = "FECHA_TERMINO")
    private LocalDate fechaTermino;

    @Column(name = "IDA_VUELTA")
    private Integer idaVuelta;

    @Column(name = "TIPO_SERVICIO_ID", length = 36)
    private String tipoServicioId;

    @Column(name = "ESTADO", length = 20)
    private String estado;

    @Column(name = "VALOR_NETO", precision = 14, scale = 2)
    private BigDecimal valorNeto;

    @Column(name = "IVA", precision = 14, scale = 2)
    private BigDecimal iva;

    @Column(name = "VALOR_TOTAL", precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "TIPO_DOCUMENTO", length = 30)
    private String tipoDocumento;

    @Column(name = "NUM_DOCUMENTO", length = 50)
    private String numDocumento;

    @Column(name = "FECHA_FACTURA")
    private LocalDate fechaFactura;

    @Column(name = "FACTURADO")
    private Integer facturado;

    @Column(name = "FACTURA_ID", length = 36)
    private String facturaId;

    @Column(name = "NOTAS", length = 2000)
    private String notas;

    @Column(name = "ELIMINADO")
    private Integer eliminado;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (estado == null)    estado    = "PENDIENTE";
        if (facturado == null) facturado = 0;
        if (idaVuelta == null) idaVuelta = 0;
        if (eliminado == null) eliminado = 0;
        if (valorNeto  == null) valorNeto  = BigDecimal.ZERO;
        if (iva        == null) iva        = BigDecimal.ZERO;
        if (valorTotal == null) valorTotal = BigDecimal.ZERO;
        if (fechaServicio == null) fechaServicio = LocalDate.now();
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
