package cl.truckmanager.vehiculos.service;

import cl.truckmanager.vehiculos.dto.VehiculoDto;
import cl.truckmanager.vehiculos.entity.Vehiculo;
import cl.truckmanager.vehiculos.repository.VehiculoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VehiculoService {

    private final VehiculoRepository repositorio;

    public Page<Vehiculo> obtenerTodos(String idEmpresa, String estado, String busqueda, int pagina, int tamano) {
        return repositorio.buscarPorFiltros(
            idEmpresa,
            (estado   != null && !estado.isBlank())   ? estado   : null,
            (busqueda != null && !busqueda.isBlank()) ? busqueda : null,
            PageRequest.of(pagina, tamano, Sort.by("patente"))
        );
    }

    /** Uso interno (sin validación de empresa) */
    public Vehiculo obtenerPorId(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vehículo no encontrado: " + id));
    }

    /** Uso externo: valida que el vehículo pertenezca a la empresa del JWT */
    public Vehiculo obtenerPorId(String id, String empresaId) {
        return repositorio.findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
    }

    public Vehiculo obtenerPorQr(String codigoQr) {
        return repositorio.findByQrCode(codigoQr)
            .orElseThrow(() -> new EntityNotFoundException("QR no válido: " + codigoQr));
    }

    public Vehiculo crear(String idEmpresa, VehiculoDto datos) {
        return repositorio.save(Vehiculo.builder()
            .id(UUID.randomUUID().toString())
            .empresaId(idEmpresa)
            .patente(datos.getPatente())
            .marca(datos.getMarca())
            .modelo(datos.getModelo())
            .anio(datos.getAnio())
            .tipo(datos.getTipo())
            .combustible(datos.getCombustible())
            .estado(datos.getEstado())
            .kmActuales(datos.getKmActuales())
            .kmProximoServicio(datos.getKmProximoServicio())
            .vencimientoRevision(datos.getVencimientoRevision())
            .vencimientoPermiso(datos.getVencimientoPermiso())
            .color(datos.getColor())
            .numMotor(datos.getNumMotor())
            .numChasis(datos.getNumChasis())
            .qrCode(datos.getQrCode() != null ? datos.getQrCode() : UUID.randomUUID().toString())
            .capacidadEstanque(datos.getCapacidadEstanque())
            .taraKg(datos.getTaraKg())
            .capacidadCargaKg(datos.getCapacidadCargaKg())
            .condicion(datos.getCondicion())
            .valorCompra(datos.getValorCompra())
            .fechaCompra(datos.getFechaCompra())
            .paisOrigen(datos.getPaisOrigen())
            .estadoOperacion(datos.getEstadoOperacion())
            .sucursalId(datos.getSucursalId())
            .usaAdBlue(datos.getUsaAdBlue() != null ? datos.getUsaAdBlue() : 0)
            .normaEuro(datos.getNormaEuro())
            .build());
    }

    public Vehiculo actualizar(String id, String empresaId, VehiculoDto datos) {
        Vehiculo v = obtenerPorId(id, empresaId);
        if (datos.getPatente()            != null) v.setPatente(datos.getPatente());
        if (datos.getMarca()              != null) v.setMarca(datos.getMarca());
        if (datos.getModelo()             != null) v.setModelo(datos.getModelo());
        if (datos.getAnio()               != null) v.setAnio(datos.getAnio());
        if (datos.getTipo()               != null) v.setTipo(datos.getTipo());
        if (datos.getCombustible()        != null) v.setCombustible(datos.getCombustible());
        if (datos.getEstado()             != null) v.setEstado(datos.getEstado());
        if (datos.getKmActuales()         != null) v.setKmActuales(datos.getKmActuales());
        if (datos.getKmProximoServicio()  != null) v.setKmProximoServicio(datos.getKmProximoServicio());
        if (datos.getVencimientoRevision()!= null) v.setVencimientoRevision(datos.getVencimientoRevision());
        if (datos.getVencimientoPermiso() != null) v.setVencimientoPermiso(datos.getVencimientoPermiso());
        if (datos.getColor()              != null) v.setColor(datos.getColor());
        if (datos.getQrCode()             != null) v.setQrCode(datos.getQrCode());
        if (datos.getCapacidadEstanque()  != null) v.setCapacidadEstanque(datos.getCapacidadEstanque());
        if (datos.getTaraKg()             != null) v.setTaraKg(datos.getTaraKg());
        if (datos.getCapacidadCargaKg()   != null) v.setCapacidadCargaKg(datos.getCapacidadCargaKg());
        if (datos.getCondicion()          != null) v.setCondicion(datos.getCondicion());
        if (datos.getValorCompra()        != null) v.setValorCompra(datos.getValorCompra());
        if (datos.getFechaCompra()        != null) v.setFechaCompra(datos.getFechaCompra());
        if (datos.getPaisOrigen()         != null) v.setPaisOrigen(datos.getPaisOrigen());
        if (datos.getEstadoOperacion()    != null) v.setEstadoOperacion(datos.getEstadoOperacion());
        if (datos.getSucursalId()         != null) v.setSucursalId(datos.getSucursalId());
        if (datos.getUsaAdBlue()          != null) v.setUsaAdBlue(datos.getUsaAdBlue());
        if (datos.getNormaEuro()          != null) v.setNormaEuro(datos.getNormaEuro());
        return repositorio.save(v);
    }

    public void eliminar(String id, String empresaId) {
        Vehiculo v = obtenerPorId(id, empresaId);
        v.setEliminado(1);
        repositorio.save(v);
    }
}
