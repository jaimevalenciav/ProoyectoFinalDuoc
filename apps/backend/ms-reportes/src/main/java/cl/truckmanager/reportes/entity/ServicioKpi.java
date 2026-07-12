package cl.truckmanager.reportes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "SERVICIOS")
@Immutable
@Data
@NoArgsConstructor
public class ServicioKpi {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36)
    private String empresaId;

    @Column(name = "VEHICULO_ID", length = 36)
    private String vehiculoId;

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

    @Column(name = "ESTADO", length = 20)
    private String estado;

    @Column(name = "VALOR_NETO", precision = 14, scale = 2)
    private BigDecimal valorNeto;

    @Column(name = "VALOR_TOTAL", precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "ELIMINADO")
    private Integer eliminado;
}
