package cl.truckmanager.reportes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "CARGAS_COMBUSTIBLE")
@Immutable
@Data
@NoArgsConstructor
public class CargaCombustibleKpi {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36)
    private String empresaId;

    @Column(name = "VEHICULO_ID", length = 36)
    private String vehiculoId;

    @Column(name = "FECHA_CARGA")
    private LocalDate fechaCarga;

    @Column(name = "LITROS", precision = 10, scale = 2)
    private BigDecimal litros;

    @Column(name = "COSTO_TOTAL", precision = 14, scale = 2)
    private BigDecimal costoTotal;

    @Column(name = "KM_VEHICULO")
    private Long kmVehiculo;

    @Column(name = "TIPO_COMBUSTIBLE", length = 30)
    private String tipoCombustible;

    @Column(name = "PROVEEDOR", length = 100)
    private String proveedor;

    @Column(name = "PRECIO_LITRO", precision = 10, scale = 2)
    private BigDecimal precioLitro;
}
