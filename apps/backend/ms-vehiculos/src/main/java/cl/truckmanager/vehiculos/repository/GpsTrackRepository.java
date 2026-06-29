package cl.truckmanager.vehiculos.repository;

import cl.truckmanager.vehiculos.entity.GpsTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GpsTrackRepository extends JpaRepository<GpsTrack, Long> {

    /** Última posición de cada vehículo de la empresa */
    @Query("""
        SELECT g FROM GpsTrack g
        WHERE g.empresaId = :empresaId
          AND g.recordedAt = (
              SELECT MAX(g2.recordedAt) FROM GpsTrack g2
              WHERE g2.vehiculoId = g.vehiculoId
          )
        ORDER BY g.vehiculoId
        """)
    List<GpsTrack> findUltimasPosiciones(@Param("empresaId") String empresaId);

    /** Recorrido de un vehículo en un rango de tiempo */
    @Query("""
        SELECT g FROM GpsTrack g
        WHERE g.vehiculoId = :vehiculoId
          AND (:desde IS NULL OR g.recordedAt >= :desde)
          AND (:hasta IS NULL OR g.recordedAt <= :hasta)
        ORDER BY g.recordedAt ASC
        """)
    List<GpsTrack> findRecorrido(
        @Param("vehiculoId") String vehiculoId,
        @Param("desde")      LocalDateTime desde,
        @Param("hasta")      LocalDateTime hasta
    );

    /** Para insertar nueva posición simulada */
    Optional<GpsTrack> findTopByVehiculoIdOrderByRecordedAtDesc(String vehiculoId);
}
