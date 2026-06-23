package cl.fleetmanager.bffmobile.repository;

import cl.fleetmanager.bffmobile.entity.GpsTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpsTrackRepository extends JpaRepository<GpsTrack, Long> {
}
