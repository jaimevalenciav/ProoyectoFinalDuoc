package cl.truckmanager.taller.repository;

import cl.truckmanager.taller.entity.OrdenTrabajo;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, String> {

    @Query("SELECT o FROM OrdenTrabajo o WHERE o.empresaId = :idEmpresa AND o.eliminado = 0 " +
           "AND (:estado IS NULL OR o.estado = :estado) " +
           "AND (:tipo   IS NULL OR o.tipo   = :tipo) " +
           "AND (:vehiculoId IS NULL OR o.vehiculoId = :vehiculoId)")
    Page<OrdenTrabajo> buscarPorFiltros(
        @Param("idEmpresa")  String idEmpresa,
        @Param("estado")     String estado,
        @Param("tipo")       String tipo,
        @Param("vehiculoId") String vehiculoId,
        Pageable pageable
    );

    @Query("SELECT COALESCE(MAX(o.numero), 'OT-0000') FROM OrdenTrabajo o WHERE o.empresaId = :idEmpresa")
    String ultimoNumero(@Param("idEmpresa") String idEmpresa);
}
