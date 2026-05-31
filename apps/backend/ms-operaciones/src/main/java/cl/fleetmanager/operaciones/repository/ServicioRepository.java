package cl.fleetmanager.operaciones.repository;

import cl.fleetmanager.operaciones.entity.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, String> {

    @Query("SELECT s FROM Servicio s WHERE s.empresaId = :empresaId AND s.eliminado = 0 " +
           "AND (:estado    IS NULL OR s.estado    = :estado) " +
           "AND (:clienteId IS NULL OR s.clienteId = :clienteId) " +
           "AND (:desde     IS NULL OR s.fechaServicio >= :desde) " +
           "AND (:hasta     IS NULL OR s.fechaServicio <= :hasta) " +
           "AND (:facturado IS NULL OR s.facturado = :facturado)")
    Page<Servicio> buscarPorFiltros(
        @Param("empresaId") String empresaId,
        @Param("estado")    String estado,
        @Param("clienteId") String clienteId,
        @Param("desde")     LocalDate desde,
        @Param("hasta")     LocalDate hasta,
        @Param("facturado") Integer facturado,
        Pageable pageable
    );

    @Query("SELECT COALESCE(MAX(s.numServicio), 'SRV-0000') FROM Servicio s WHERE s.empresaId = :empresaId")
    String ultimoNumero(@Param("empresaId") String empresaId);

    List<Servicio> findByIdInAndEmpresaId(List<String> ids, String empresaId);

    List<Servicio> findByFacturaId(String facturaId);

    List<Servicio> findByConductorIdAndEliminadoOrderByFechaServicioDesc(String conductorId, int eliminado);
}
