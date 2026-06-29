package cl.truckmanager.conductores.service;

import cl.truckmanager.conductores.dto.ConductorDto;
import cl.truckmanager.conductores.entity.Conductor;
import cl.truckmanager.conductores.repository.ConductorRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConductorService — pruebas unitarias")
class ConductorServiceTest {

    @Mock
    private ConductorRepository repositorio;

    @InjectMocks
    private ConductorService servicio;

    private static final String EMPRESA_ID   = "EMP-001";
    private static final String CONDUCTOR_ID = "CON-001";

    private Conductor conductorEjemplo;

    @BeforeEach
    void configurar() {
        conductorEjemplo = new Conductor();
        conductorEjemplo.setId(CONDUCTOR_ID);
        conductorEjemplo.setEmpresaId(EMPRESA_ID);
        conductorEjemplo.setNombre("Juan Pérez");
        conductorEjemplo.setRut("12345678-9");
        conductorEjemplo.setEmail("juan@truckmanager.cl");
        conductorEjemplo.setCategoriaLicencia("A3");
        conductorEjemplo.setVencimientoLicencia(LocalDate.of(2027, 6, 30));
        conductorEjemplo.setEstado("ACTIVO");
        conductorEjemplo.setEliminado(0);
        conductorEjemplo.setScoreConduccion(95);
    }

    // ─── obtenerTodos ────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerTodos devuelve pagina filtrada por empresa")
    void obtenerTodos_devuelveResultados() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), any()))
            .thenReturn(new PageImpl<>(List.of(conductorEjemplo)));

        Page<Conductor> resultado = servicio.obtenerTodos(EMPRESA_ID, null, null, 0, 50);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("obtenerTodos con estado en blanco pasa null al repositorio")
    void obtenerTodos_estadoEnBlanco_pasaNull() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), any()))
            .thenReturn(Page.empty());

        servicio.obtenerTodos(EMPRESA_ID, "  ", "  ", 0, 50);

        verify(repositorio).buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), any());
    }

    // ─── obtenerPorId (sin empresa) ──────────────────────────────────────────

    @Test
    @DisplayName("obtenerPorId retorna conductor cuando existe")
    void obtenerPorId_existente_retornaConductor() {
        when(repositorio.findById(CONDUCTOR_ID)).thenReturn(Optional.of(conductorEjemplo));

        Conductor resultado = servicio.obtenerPorId(CONDUCTOR_ID);

        assertThat(resultado.getId()).isEqualTo(CONDUCTOR_ID);
    }

    @Test
    @DisplayName("obtenerPorId lanza EntityNotFoundException cuando no existe")
    void obtenerPorId_inexistente_lanzaExcepcion() {
        when(repositorio.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId("X"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── obtenerPorId (con empresa) ──────────────────────────────────────────

    @Test
    @DisplayName("obtenerPorId con empresa retorna conductor cuando pertenece a la empresa")
    void obtenerPorId_conEmpresa_retornaConductor() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(CONDUCTOR_ID, EMPRESA_ID, 0))
            .thenReturn(Optional.of(conductorEjemplo));

        Conductor resultado = servicio.obtenerPorId(CONDUCTOR_ID, EMPRESA_ID);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("obtenerPorId con empresa lanza 404 cuando el conductor es de otra empresa")
    void obtenerPorId_empresaAjena_lanza404() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(CONDUCTOR_ID, "EMP-999", 0))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(CONDUCTOR_ID, "EMP-999"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    // ─── crear ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("crear persiste conductor con estado ACTIVO por defecto")
    void crear_sinEstado_asignaActivo() {
        ConductorDto dto = new ConductorDto();
        dto.setNombre("Pedro Gonzalez");
        dto.setRut("98765432-1");
        dto.setCategoriaLicencia("B");
        dto.setVencimientoLicencia(LocalDate.of(2026, 12, 31));

        when(repositorio.save(any(Conductor.class))).thenAnswer(inv -> {
            Conductor c = inv.getArgument(0);
            c.setId(CONDUCTOR_ID);
            return c;
        });

        Conductor resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getEstado()).isEqualTo("ACTIVO");
        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("crear respeta el estado cuando el dto lo especifica")
    void crear_conEstado_respetaEstado() {
        ConductorDto dto = new ConductorDto();
        dto.setNombre("Luis Torres");
        dto.setRut("11111111-1");
        dto.setCategoriaLicencia("A3");
        dto.setVencimientoLicencia(LocalDate.of(2028, 1, 1));
        dto.setEstado("INACTIVO");

        when(repositorio.save(any(Conductor.class))).thenAnswer(inv -> inv.getArgument(0));

        Conductor resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getEstado()).isEqualTo("INACTIVO");
    }

    // ─── actualizar ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("actualizar modifica solo los campos enviados en el dto")
    void actualizar_camposSeleccionados_actualizaCorrectamente() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(CONDUCTOR_ID, EMPRESA_ID, 0))
            .thenReturn(Optional.of(conductorEjemplo));
        when(repositorio.save(any(Conductor.class))).thenAnswer(inv -> inv.getArgument(0));

        ConductorDto dto = new ConductorDto();
        dto.setTelefono("+56912345678");
        dto.setEstado("INACTIVO");

        Conductor resultado = servicio.actualizar(CONDUCTOR_ID, EMPRESA_ID, dto);

        assertThat(resultado.getTelefono()).isEqualTo("+56912345678");
        assertThat(resultado.getEstado()).isEqualTo("INACTIVO");
        assertThat(resultado.getNombre()).isEqualTo("Juan Pérez"); // no cambio
    }

    @Test
    @DisplayName("actualizar lanza 404 cuando el conductor pertenece a otra empresa")
    void actualizar_empresaAjena_lanza404() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(CONDUCTOR_ID, "EMP-999", 0))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(CONDUCTOR_ID, "EMP-999", new ConductorDto()))
            .isInstanceOf(ResponseStatusException.class);
        verify(repositorio, never()).save(any());
    }

    // ─── eliminar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar marca el conductor como eliminado logicamente")
    void eliminar_conductorPropio_marcaEliminado() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(CONDUCTOR_ID, EMPRESA_ID, 0))
            .thenReturn(Optional.of(conductorEjemplo));
        when(repositorio.save(any(Conductor.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio.eliminar(CONDUCTOR_ID, EMPRESA_ID);

        assertThat(conductorEjemplo.getEliminado()).isEqualTo(1);
        verify(repositorio).save(conductorEjemplo);
    }

    @Test
    @DisplayName("eliminar lanza 404 cuando el conductor es de otra empresa")
    void eliminar_empresaAjena_lanza404() {
        when(repositorio.findByIdAndEmpresaIdAndEliminado(CONDUCTOR_ID, "EMP-999", 0))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(CONDUCTOR_ID, "EMP-999"))
            .isInstanceOf(ResponseStatusException.class);
        verify(repositorio, never()).save(any());
    }

    // ─── obtenerScore ────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerScore devuelve el score actual del conductor")
    void obtenerScore_conductorConScore_retornaScore() {
        conductorEjemplo.setScoreConduccion(87);
        when(repositorio.findById(CONDUCTOR_ID)).thenReturn(Optional.of(conductorEjemplo));

        Map<String, Object> resultado = servicio.obtenerScore(CONDUCTOR_ID);

        assertThat(resultado.get("score")).isEqualTo(87);
        assertThat(resultado).containsKey("detalle");
    }

    @Test
    @DisplayName("obtenerScore devuelve 100 cuando el conductor no tiene score registrado")
    void obtenerScore_sinScore_retorna100() {
        conductorEjemplo.setScoreConduccion(null);
        when(repositorio.findById(CONDUCTOR_ID)).thenReturn(Optional.of(conductorEjemplo));

        Map<String, Object> resultado = servicio.obtenerScore(CONDUCTOR_ID);

        assertThat(resultado.get("score")).isEqualTo(100);
    }
}
