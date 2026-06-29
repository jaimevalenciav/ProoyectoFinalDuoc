package cl.truckmanager.reportes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;

@Entity
@Table(name = "REPUESTOS")
@Immutable
@Data
@NoArgsConstructor
public class RepuestoKpi {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36)
    private String empresaId;

    @Column(name = "STOCK_ACTUAL", precision = 10, scale = 2)
    private BigDecimal stockActual;

    @Column(name = "STOCK_MINIMO", precision = 10, scale = 2)
    private BigDecimal stockMinimo;

    @Column(name = "ELIMINADO")
    private Integer eliminado;
}
