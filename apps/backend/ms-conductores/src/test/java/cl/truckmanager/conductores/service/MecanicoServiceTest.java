package cl.truckmanager.conductores.service;

import cl.truckmanager.conductores.dto.MecanicoDto;
import cl.truckmanager.conductores.entity.Mecanico;
import cl.truckmanager.conductores.repository.MecanicoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MecanicoService — pruebas unitarias")
class MecanicoServiceTest {

    @Mock
    private MecanicoRepository repositorio;

    @InjectMocks
    private MecanicoService servicio;

    private static final String EMPRESA_ID  = "EMP-001";
    private static final String MECANICO_ID = "MEC-001";

    private Mecanico mecanicoEjemplo;

    @BeforeEach
    void configurar() {
        mecanicoEjemplo = Mecanico.builder()
            .id(MECANICO_ID)
            .empresaId(EMPRESA_ID)
            .nombre("Carlos Soto")
            .rut("11111111-1")
            .especialidad("Motor")
            .activo(1)
            .build();
    }

    // ─── obtenerTodos ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerTodos devuelve pagina filtrada por empresa")
    void obtenerTodos_devuelveResultados() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), any()))
            .thenReturn(new PageImpl<>(List.of(mecanicoEjemplo)));

        Page<Mecanico> resultado = servicio.obtenerTodos(EMPRESA_ID, null, null, 0, 20);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("obtenerTodos con busqueda en blanco la convierte a null")
    void obtenerTodos_busquedaEnBlanco_pasaNull() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), any()))
            .thenReturn(Page.empty());

        servicio.obtenerTodos(EMPRESA_ID, "  ", null, 0, 20);

        verify(repositorio).buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), any());
    }

    // ─── obtenerActivos ───────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerActivos retorna solo mecanicos con activo=1")
    void obtenerActivos_retornaActivos() {
        when(repositorio.findByEmpresaIdAndActivoOrderByNombre(EMPRESA_ID, 1))
            .thenReturn(List.of(mecanicoEjemplo));

        List<Mecanico> resultado = servicio.obtenerActivos(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getActivo()).isEqualTo(1);
    }

    // ─── obtenerPorId ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerPorId retorna mecanico cuando existe")
    void obtenerPorId_existente_retornaMecanico() {
        when(repositorio.findById(MECANICO_ID)).thenReturn(Optional.of(mecanicoEjemplo));

        Mecanico resultado = servicio.obtenerPorId(MECANICO_ID);

        assertThat(resultado.getId()).isEqualTo(MECANICO_ID);
    }

    @Test
    @DisplayName("obtenerPorId lanza EntityNotFoundException cuando no existe")
    void obtenerPorId_inexistente_lanzaExcepcion() {
        when(repositorio.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId("X"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── crear ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("crear persiste mecanico con activo=1 por defecto")
    void crear_sinActivo_asignaUno() {
        when(repositorio.save(any(Mecanico.class))).thenReturn(mecanicoEjemplo);

        MecanicoDto dto = new MecanicoDto();
        dto.setNombre("Carlos Soto");
        dto.setRut("11111111-1");

        Mecanico resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(resultado.getActivo()).isEqualTo(1);
    }

    @Test
    @DisplayName("crear respeta activo cuando el dto lo especifica")
    void crear_conActivo0_persisteActivo0() {
        Mecanico inactivo = Mecanico.builder().id("M-2").empresaId(EMPRESA_ID)
            .nombre("Test").activo(0).build();
        when(repositorio.save(any(Mecanico.class))).thenReturn(inactivo);

        MecanicoDto dto = new MecanicoDto();
        dto.setNombre("Test");
        dto.setActivo(0);

        Mecanico resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getActivo()).isEqualTo(0);
    }

    // ─── actualizar ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("actualizar modifica los campos enviados en el dto")
    void actualizar_camposSeleccionados_actualizaCorrectamente() {
        when(repositorio.findById(MECANICO_ID)).thenReturn(Optional.of(mecanicoEjemplo));
        when(repositorio.save(any(Mecanico.class))).thenAnswer(inv -> inv.getArgument(0));

        MecanicoDto dto = new MecanicoDto();
        dto.setNombre("Carlos Soto Ramirez");
        dto.setEspecialidad("Frenos");

        Mecanico resultado = servicio.actualizar(MECANICO_ID, dto);

        assertThat(resultado.getNombre()).isEqualTo("Carlos Soto Ramirez");
        assertThat(resultado.getEspecialidad()).isEqualTo("Frenos");
    }

    @Test
    @DisplayName("actualizar lanza EntityNotFoundException cuando no existe")
    void actualizar_inexistente_lanzaExcepcion() {
        when(repositorio.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar("X", new MecanicoDto()))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── eliminar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar desactiva el mecanico (activo=0)")
    void eliminar_mecanicoExistente_desactiva() {
        when(repositorio.findById(MECANICO_ID)).thenReturn(Optional.of(mecanicoEjemplo));

        servicio.eliminar(MECANICO_ID);

        assertThat(mecanicoEjemplo.getActivo()).isEqualTo(0);
        verify(repositorio).save(mecanicoEjemplo);
    }

    @Test
    @DisplayName("eliminar lanza EntityNotFoundException cuando no existe")
    void eliminar_inexistente_lanzaExcepcion() {
        when(repositorio.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar("X"))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
