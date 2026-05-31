package cl.fleetmanager.almacen.repository;

import cl.fleetmanager.almacen.entity.MovimientoStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, String> {

    @Query("SELECT m FROM MovimientoStock m WHERE m.empresaId = :emp AND m.repuesto.id = :repId ORDER BY m.createdAt DESC")
    Page<MovimientoStock> findByEmpresaIdAndRepuestoIdOrderByCreatedAtDesc(
            @Param("emp") String empresaId,
            @Param("repId") String repuestoId,
            Pageable pageable);

    @Query("SELECT m FROM MovimientoStock m WHERE m.empresaId = :emp ORDER BY m.createdAt DESC")
    Page<MovimientoStock> findByEmpresaIdOrderByCreatedAtDesc(
            @Param("emp") String empresaId,
            Pageable pageable);
}
