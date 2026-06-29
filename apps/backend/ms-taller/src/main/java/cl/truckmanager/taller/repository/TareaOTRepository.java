package cl.truckmanager.taller.repository;

import cl.truckmanager.taller.entity.TareaOT;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TareaOTRepository extends JpaRepository<TareaOT, String> {
    List<TareaOT> findByOrdenTrabajoIdOrderByOrdenAsc(String otId);
}
