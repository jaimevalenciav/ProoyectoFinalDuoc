package cl.truckmanager.bffmobile.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "VEHICULOS")
@Data
@NoArgsConstructor
public class Vehiculo {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "PATENTE", length = 10)
    private String patente;

    @Column(name = "MARCA", length = 100)
    private String marca;

    @Column(name = "MODELO", length = 100)
    private String modelo;

    @Column(name = "QR_CODE", length = 100)
    private String qrCode;

    @Column(name = "EMPRESA_ID", length = 36)
    private String empresaId;
}
