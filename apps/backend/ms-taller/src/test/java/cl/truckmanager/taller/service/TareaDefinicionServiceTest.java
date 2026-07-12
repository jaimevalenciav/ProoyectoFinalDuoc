package cl.truckmanager.taller.service;

import cl.truckmanager.taller.dto.TareaDefinicionDto;
import cl.truckmanager.taller.entity.TareaDefinicion;
import cl.truckmanager.taller.repository.TareaDefinicionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TareaDefinicionService — pruebas unitarias")
class TareaDefinicionServiceTest {

    @Mock private TareaDefinicionRepository repo;

    @InjectMocks private TareaDefinicionService servicio;

    private static final String EMP  = "EMP-001";
    private static final String TD_ID = "TD-001";

    private TareaDefinicion tarea;

    @BeforeEach
    void setUp() {
        tarea = TareaDefinicion.builder()
                .id(TD_ID)
                .empresaId(EMP)
                .nombre("Cambio de aceite")
                .descripcion("Cambio de aceite y filtro")
                .tipoOt("MANTENCION")
                .activo(1)
                .build();
    }

    @Test
    @DisplayName("getAll retorna pagina de tareas activas")
    void getAll_retornaPagina() {
        Page<TareaDefinicion> pagina = new PageImpl<>(List.of(tarea));
        when(repo.findByEmpresaIdAndActivoOrderByNombreAsc(eq(EMP), eq(1), any(PageRequest.class)))
                .thenReturn(pagina);

        Page<TareaDefinicion> resultado = servicio.getAll(EMP, 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNombre()).isEqualTo("Cambio de aceite");
    }

    @Test
    @DisplayName("getAllActivos retorna lista de tareas activas")
    void getAllActivos_retornaLista() {
        when(repo.findByEmpresaIdAndActivoOrderByNombreAsc(EMP, 1)).thenReturn(List.of(tarea));

        List<TareaDefinicion> resultado = servicio.getAllActivos(EMP);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("getById retorna tarea existente")
    void getById_retornaTarea() {
        when(repo.findById(TD_ID)).thenReturn(Optional.of(tarea));

        TareaDefinicion resultado = servicio.getById(TD_ID);

        assertThat(resultado.getId()).isEqualTo(TD_ID);
        assertThat(resultado.getNombre()).isEqualTo("Cambio de aceite");
    }

    @Test
    @DisplayName("getById lanza EntityNotFoundException si no existe")
    void getById_lanzaExcepcionSiNoExiste() {
        when(repo.findById(TD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.getById(TD_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(TD_ID);
    }

    @Test
    @DisplayName("crear guarda nueva tarea de definicion")
    void crear_guardaTarea() {
        TareaDefinicionDto dto = new TareaDefinicionDto();
        dto.setNombre("Revision de frenos");
        dto.setDescripcion("Inspección y ajuste de frenos");
        dto.setTipoOt("REPARACION");

        when(repo.save(any(TareaDefinicion.class))).thenReturn(tarea);

        TareaDefinicion resultado = servicio.crear(EMP, dto);

        assertThat(resultado).isNotNull();
        verify(repo).save(any(TareaDefinicion.class));
    }
}
