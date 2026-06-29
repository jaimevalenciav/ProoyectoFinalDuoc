package cl.truckmanager.almacen.repository;

import cl.truckmanager.almacen.entity.MovimientoStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, String> {

    @Query("SELECT m FROM MovimientoStock m WHERE m.empresaId = :emp " +
           "AND (:repId IS NULL OR m.repuesto.id = :repId) " +
           "AND (:tipo IS NULL OR m.tipo = :tipo) " +
           "AND (:doc IS NULL OR LOWER(m.documento) LIKE LOWER(CONCAT('%',:doc,'%'))) " +
           "AND (:desde IS NULL OR m.createdAt >= :desde) " +
           "AND (:hasta IS NULL OR m.createdAt <= :hasta) " +
           "ORDER BY m.createdAt DESC")
    Page<MovimientoStock> buscar(
            @Param("emp")   String empresaId,
            @Param("repId") String repuestoId,
            @Param("tipo")  String tipo,
            @Param("doc")   String documento,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta,
            Pageable pageable);
}
