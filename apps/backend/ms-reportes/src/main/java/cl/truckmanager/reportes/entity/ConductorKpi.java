package cl.truckmanager.reportes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import java.time.LocalDate;

@Entity
@Table(name = "CONDUCTORES")
@Immutable
@Data
@NoArgsConstructor
public class ConductorKpi {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36)
    private String empresaId;

    @Column(name = "ESTADO", length = 30)
    private String estado;

    @Column(name = "VENCIMIENTO_LICENCIA")
    private LocalDate vencimientoLicencia;
}
