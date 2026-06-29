package cl.truckmanager.bffmobile.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "SERVICIOS")
@Data
@NoArgsConstructor
public class Servicio {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "CONDUCTOR_ID", length = 36)
    private String conductorId;

    @Column(name = "VEHICULO_ID", length = 36)
    private String vehiculoId;

    @Column(name = "CLIENTE_ID", length = 36)
    private String clienteId;

    @Column(name = "TIPO_SERVICIO_ID", length = 36)
    private String tipoServicioId;

    @Column(name = "NUM_SERVICIO", length = 20)
    private String numServicio;

    @Column(name = "ORIGEN", length = 200)
    private String origen;

    @Column(name = "DESTINO", length = 200)
    private String destino;

    @Column(name = "ESTADO", length = 20)
    private String estado;

    @Column(name = "FECHA_SERVICIO")
    private LocalDate fechaServicio;
}
