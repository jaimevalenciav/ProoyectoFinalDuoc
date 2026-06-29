package cl.truckmanager.bffmobile.repository;

import cl.truckmanager.bffmobile.entity.GpsTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpsTrackRepository extends JpaRepository<GpsTrack, Long> {
}
