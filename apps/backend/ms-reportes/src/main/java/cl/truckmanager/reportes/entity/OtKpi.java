package cl.truckmanager.reportes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import java.time.LocalDate;

@Entity
@Table(name = "ORDENES_TRABAJO")
@Immutable
@Data
@NoArgsConstructor
public class OtKpi {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EMPRESA_ID", length = 36)
    private String empresaId;

    @Column(name = "ESTADO", length = 20)
    private String estado;

    @Column(name = "TIPO", length = 20)
    private String tipo;

    @Column(name = "FECHA_APERTURA")
    private LocalDate fechaApertura;

    @Column(name = "ELIMINADO")
    private Integer eliminado;
}
