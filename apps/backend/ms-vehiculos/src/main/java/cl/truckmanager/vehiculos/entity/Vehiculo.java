package cl.truckmanager.vehiculos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "VEHICULOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36, nullable = false)
    private String empresaId;

    @Column(name = "PATENTE", length = 10, nullable = false)
    private String patente;

    @Column(name = "MARCA", length = 100, nullable = false)
    private String marca;

    @Column(name = "MODELO", length = 100, nullable = false)
    private String modelo;

    @Column(name = "ANIO", nullable = false)
    private Integer anio;

    @Column(name = "TIPO", length = 30, nullable = false)
    private String tipo;

    @Column(name = "COMBUSTIBLE", length = 20)
    private String combustible;

    @Column(name = "ESTADO", length = 30)
    private String estado;

    @Column(name = "KM_ACTUALES")
    private Long kmActuales;

    @Column(name = "KM_PROXIMO_SERVICIO")
    private Long kmProximoServicio;

    @Column(name = "VENCIMIENTO_REVISION")
    private LocalDate vencimientoRevision;

    @Column(name = "VENCIMIENTO_PERMISO")
    private LocalDate vencimientoPermiso;

    @Column(name = "COLOR", length = 50)
    private String color;

    @Column(name = "NUM_MOTOR", length = 50)
    private String numMotor;

    @Column(name = "NUM_CHASIS", length = 50)
    private String numChasis;

    @Column(name = "QR_CODE", length = 100, unique = true)
    private String qrCode;

    @Column(name = "CAPACIDAD_ESTANQUE")
    private Integer capacidadEstanque;   // litros

    @Column(name = "TARA_KG")
    private Integer taraKg;              // kg

    @Column(name = "CAPACIDAD_CARGA_KG")
    private Integer capacidadCargaKg;   // kg

    /** NUEVO | USADO */
    @Column(name = "CONDICION", length = 10)
    private String condicion;

    @Column(name = "VALOR_COMPRA")
    private Long valorCompra;

    @Column(name = "FECHA_COMPRA")
    private java.time.LocalDate fechaCompra;

    @Column(name = "PAIS_ORIGEN", length = 50)
    private String paisOrigen;

    /** EN_OPERACION | EN_MANTENCION | FUERA_SERVICIO | BAJA */
    @Column(name = "ESTADO_OPERACION", length = 20)
    private String estadoOperacion;

    @Column(name = "SUCURSAL_ID", length = 36)
    private String sucursalId;

    /** 0 = no usa AdBlue, 1 = usa AdBlue */
    @Column(name = "USA_ADBLUE")
    private Integer usaAdBlue;

    /** EURO_III | EURO_IV | EURO_V | EURO_VI */
    @Column(name = "NORMA_EURO", length = 10)
    private String normaEuro;

    @Column(name = "ELIMINADO")
    private Integer eliminado;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (estado == null)      estado      = "OPERATIVO";
        if (combustible == null) combustible = "DIESEL";
        if (eliminado == null)   eliminado   = 0;
        if (kmActuales == null)         kmActuales         = 0L;
        if (kmProximoServicio == null)  kmProximoServicio  = 0L;
        if (usaAdBlue == null)          usaAdBlue          = 0;
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
