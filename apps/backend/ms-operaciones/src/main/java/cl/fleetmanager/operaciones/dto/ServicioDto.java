package cl.fleetmanager.operaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ServicioDto {
    @NotBlank String clienteId;
    String vehiculoId;
    String conductorId;
    @NotBlank String origen;
    @NotBlank String destino;
    BigDecimal kmsRecorrido;
    @NotNull  LocalDate fechaServicio;
    LocalDate fechaTermino;
    Integer   idaVuelta;
    String    tipoServicioId;
    String    estado;
    @NotNull  BigDecimal valorNeto;
    String    tipoDocumento;
    String    numDocumento;
    LocalDate fechaFactura;
    String    notas;
}
