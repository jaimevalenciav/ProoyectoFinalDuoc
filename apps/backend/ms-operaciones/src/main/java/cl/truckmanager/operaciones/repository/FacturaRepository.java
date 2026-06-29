package cl.truckmanager.operaciones.repository;

import cl.truckmanager.operaciones.entity.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaRepository extends JpaRepository<Factura, String> {

    @Query("SELECT f FROM Factura f WHERE f.empresaId = :empresaId " +
           "AND (:clienteId IS NULL OR f.clienteId = :clienteId) " +
           "AND (:estado    IS NULL OR f.estado    = :estado)")
    Page<Factura> buscarPorFiltros(
        @Param("empresaId") String empresaId,
        @Param("clienteId") String clienteId,
        @Param("estado")    String estado,
        Pageable pageable
    );

    @Query("SELECT COALESCE(MAX(f.numFactura), 'FAC-0000') FROM Factura f WHERE f.empresaId = :empresaId")
    String ultimoNumero(@Param("empresaId") String empresaId);
}
