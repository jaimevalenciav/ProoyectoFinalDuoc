package cl.fleetmanager.conductores.repository;

import cl.fleetmanager.conductores.entity.Conductor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConductorRepository extends JpaRepository<Conductor, String> {

    @Query("SELECT c FROM Conductor c " +
           "WHERE c.empresaId = :empresaId " +
           "AND c.eliminado = 0 " +
           "AND (:estado IS NULL OR c.estado = :estado) " +
           "AND (:busqueda IS NULL " +
           "     OR LOWER(c.nombre) LIKE LOWER(CONCAT('%',:busqueda,'%')) " +
           "     OR LOWER(c.rut)    LIKE LOWER(CONCAT('%',:busqueda,'%')))")
    Page<Conductor> buscarPorFiltros(
        @Param("empresaId") String empresaId,
        @Param("estado")    String estado,
        @Param("busqueda")  String busqueda,
        Pageable pageable
    );
}
