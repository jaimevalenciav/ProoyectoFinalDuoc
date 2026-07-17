package cl.truckmanager.conductores.repository;

import cl.truckmanager.conductores.entity.Conductor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConductorRepository extends JpaRepository<Conductor, String> {

    /** Busca conductor validando empresa y que no esté eliminado — evita IDOR */
    java.util.Optional<Conductor> findByIdAndEmpresaIdAndEliminado(String id, String empresaId, Integer eliminado);

    java.util.Optional<Conductor> findByIdAndEmpresaId(String id, String empresaId);

    java.util.Optional<Conductor> findByEmailAndEliminado(String email, Integer eliminado);

    java.util.Optional<Conductor> findByUsuarioIdAndEliminado(String usuarioId, Integer eliminado);

    java.util.Optional<Conductor> findByAzureOidAndEliminado(String azureOid, Integer eliminado);

    @Query("SELECT c FROM Conductor c " +
           "WHERE c.empresaId = :empresaId " +
           "AND (:eliminado IS NULL OR c.eliminado = :eliminado) " +
           "AND (:estado IS NULL OR c.estado = :estado) " +
           "AND (:busqueda IS NULL " +
           "     OR LOWER(c.nombre) LIKE LOWER(CONCAT('%',:busqueda,'%')) " +
           "     OR LOWER(c.rut)    LIKE LOWER(CONCAT('%',:busqueda,'%')))")
    Page<Conductor> buscarPorFiltros(
        @Param("empresaId") String empresaId,
        @Param("eliminado") Integer eliminado,
        @Param("estado")    String estado,
        @Param("busqueda")  String busqueda,
        Pageable pageable
    );
}
