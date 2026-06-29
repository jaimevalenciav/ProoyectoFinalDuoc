package cl.truckmanager.almacen.repository;

import cl.truckmanager.almacen.entity.Repuesto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RepuestoRepository extends JpaRepository<Repuesto, String> {

    @Query("SELECT r FROM Repuesto r WHERE r.empresaId = :emp AND r.eliminado = 0 " +
           "AND (:cat IS NULL OR r.categoria = :cat) " +
           "AND (:q IS NULL OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(r.codigo) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "ORDER BY r.descripcion")
    Page<Repuesto> buscar(@Param("emp") String emp,
                          @Param("cat") String cat,
                          @Param("q") String q,
                          Pageable p);

    @Query("SELECT r FROM Repuesto r WHERE r.empresaId = :emp AND r.eliminado = 0 ORDER BY r.descripcion")
    List<Repuesto> findAllActivosByEmpresa(@Param("emp") String emp);

    @Query("SELECT r FROM Repuesto r WHERE r.empresaId = :emp AND r.eliminado = 0 AND r.stockActual <= r.stockMinimo")
    List<Repuesto> findBajoStock(@Param("emp") String emp);
}
