package cl.truckmanager.vehiculos.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class VehiculoDto {
    @NotBlank String patente;
    @NotBlank String marca;
    @NotBlank String modelo;
    @Min(1990) Integer anio;
    @NotBlank String tipo;
    String combustible;
    String estado;
    Long kmActuales;
    Long kmProximoServicio;
    LocalDate vencimientoRevision;
    LocalDate vencimientoPermiso;
    String color;
    String numMotor;
    String numChasis;
    String qrCode;
    Integer capacidadEstanque;
    Integer taraKg;
    Integer capacidadCargaKg;
    // Nuevos campos
    String condicion;
    Long valorCompra;
    java.time.LocalDate fechaCompra;
    String paisOrigen;
    String estadoOperacion;
    String sucursalId;
    Integer usaAdBlue;
    String normaEuro;
}
