package cl.fleetmanager.conductores.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CONDUCTORES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conductor {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "USUARIO_ID", length = 36)
    private String usuarioId;

    @Column(name = "NOMBRE", length = 200, nullable = false)
    private String nombre;

    @Column(name = "RUT", length = 20, nullable = false)
    private String rut;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "EMAIL", length = 150)
    private String email;

    @Column(name = "CATEGORIA_LICENCIA", length = 5, nullable = false)
    private String categoriaLicencia;

    @Column(name = "VENCIMIENTO_LICENCIA", nullable = false)
    private LocalDate vencimientoLicencia;

    @Column(name = "ESTADO", length = 20, nullable = false)
    private String estado;

    @Column(name = "SCORE_CONDUCCION")
    private Integer scoreConduccion;

    @Column(name = "HORAS_MES", precision = 6, scale = 1)
    private BigDecimal horasMes;

    @Column(name = "KM_MES")
    private Long kmMes;

    @Column(name = "INFRACCIONES_MES")
    private Integer infraccionesMes;

    @Column(name = "ELIMINADO")
    private Integer eliminado;

    /** OID de Azure AD B2C — usado para autenticación móvil (sin FK) */
    @Column(name = "AZURE_OID", length = 36)
    private String azureOid;

    @Lob
    @Column(name = "FOTO_BASE64", columnDefinition = "CLOB")
    private String fotoBase64;

    @Lob
    @Column(name = "LICENCIA_FRENTE", columnDefinition = "CLOB")
    private String licenciaFrente;

    @Lob
    @Column(name = "LICENCIA_REVERSO", columnDefinition = "CLOB")
    private String licenciaReverso;

    @Column(name = "VENCIMIENTO_CEDULA")
    private LocalDate vencimientoCedula;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (estado    == null) estado    = "ACTIVO";
        if (scoreConduccion == null) scoreConduccion = 100;
        if (horasMes  == null) horasMes  = BigDecimal.ZERO;
        if (kmMes     == null) kmMes     = 0L;
        if (infraccionesMes == null) infraccionesMes = 0;
        if (eliminado == null) eliminado = 0;
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
