package cl.truckmanager.vehiculos.service;

import cl.truckmanager.vehiculos.dto.VehiculoDto;
import cl.truckmanager.vehiculos.entity.Vehiculo;
import cl.truckmanager.vehiculos.repository.VehiculoRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehiculoService — pruebas unitarias")
class VehiculoServiceTest {

    @Mock
    private VehiculoRepository repositorio;

    @InjectMocks
    private VehiculoService servicio;

    private static final String EMPRESA_ID  = "EMP-001";
    private static final String VEHICULO_ID = "VEH-001";

    private Vehiculo vehiculoEjemplo;

    @BeforeEach
    void configurar() {
        vehiculoEjemplo = new Vehiculo();
        vehiculoEjemplo.setId(VEHICULO_ID);
        vehiculoEjemplo.setEmpresaId(EMPRESA_ID);
        vehiculoEjemplo.setPatente("BGJK-91");
        vehiculoEjemplo.setMarca("Volvo");
        vehiculoEjemplo.setModelo("FH16");
        vehiculoEjemplo.setAnio(2022);
        vehiculoEjemplo.setTipo("CAMION");
        vehiculoEjemplo.setEstado("OPERATIVO");
        vehiculoEjemplo.setEliminado(0);
    }

    // ─── obtenerTodos ────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerTodos devuelve pagina filtrada por empresa")
    void obtenerTodos_devuelveResultados() {
        Page<Vehiculo> paginaEsperada = new PageImpl<>(List.of(vehiculoEjemplo));
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), any()))
            .thenReturn(paginaEsperada);

        Page<Vehiculo> resultado = servicio.obtenerTodos(EMPRESA_ID, null, null, 0, 20);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getEmpresaId()).isEqualTo(EMPRESA_ID);
        verify(repositorio).buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("obtenerTodos con estado y busqueda los pasa al repositorio")
    void obtenerTodos_conFiltros_pasaParametros() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), eq("OPERATIVO"), eq("Volvo"), any()))
            .thenReturn(new PageImpl<>(List.of(vehiculoEjemplo)));

        servicio.obtenerTodos(EMPRESA_ID, "OPERATIVO", "Volvo", 0, 20);

        verify(repositorio).buscarPorFiltros(eq(EMPRESA_ID), eq("OPERATIVO"), eq("Volvo"), any());
    }

    @Test
    @DisplayName("obtenerTodos con estado en blanco lo convierte a null")
    void obtenerTodos_estadoEnBlanco_pasaNull() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), any()))
            .thenReturn(Page.empty());

        servicio.obtenerTodos(EMPRESA_ID, "  ", "", 0, 20);

        verify(repositorio).buscarPorFiltros(eq(EMPRESA_ID), isNull(), isNull(), any());
    }

    // ─── obtenerPorId (sin empresa) ──────────────────────────────────────────

    @Test
    @DisplayName("obtenerPorId retorna vehiculo cuando existe")
    void obtenerPorId_existente_retornaVehiculo() {
        when(repositorio.findById(VEHICULO_ID)).thenReturn(Optional.of(vehiculoEjemplo));

        Vehiculo resultado = servicio.obtenerPorId(VEHICULO_ID);

        assertThat(resultado.getId()).isEqualTo(VEHICULO_ID);
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
    @DisplayName("obtenerPorId con empresa retorna vehiculo cuando pertenece a la empresa")
    void obtenerPorId_conEmpresa_retornaVehiculo() {
        when(repositorio.findByIdAndEmpresaId(VEHICULO_ID, EMPRESA_ID))
            .thenReturn(Optional.of(vehiculoEjemplo));

        Vehiculo resultado = servicio.obtenerPorId(VEHICULO_ID, EMPRESA_ID);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("obtenerPorId con empresa lanza 404 cuando pertenece a otra empresa")
    void obtenerPorId_conEmpresaAjena_lanza404() {
        when(repositorio.findByIdAndEmpresaId(VEHICULO_ID, "EMP-999"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(VEHICULO_ID, "EMP-999"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    // ─── obtenerPorQr ────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerPorQr retorna vehiculo cuando el codigo existe")
    void obtenerPorQr_existente_retornaVehiculo() {
        vehiculoEjemplo.setQrCode("QR-ABC");
        when(repositorio.findByQrCode("QR-ABC")).thenReturn(Optional.of(vehiculoEjemplo));

        Vehiculo resultado = servicio.obtenerPorQr("QR-ABC");

        assertThat(resultado.getQrCode()).isEqualTo("QR-ABC");
    }

    @Test
    @DisplayName("obtenerPorQr lanza EntityNotFoundException cuando el QR no existe")
    void obtenerPorQr_inexistente_lanzaExcepcion() {
        when(repositorio.findByQrCode("QR-X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorQr("QR-X"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── crear ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("crear persiste vehiculo con empresaId del JWT")
    void crear_datosValidos_guardaConEmpresaId() {
        VehiculoDto dto = new VehiculoDto();
        dto.setPatente("BGJK-91");
        dto.setMarca("Volvo");
        dto.setModelo("FH16");
        dto.setAnio(2022);
        dto.setTipo("CAMION");

        when(repositorio.save(any(Vehiculo.class))).thenAnswer(inv -> {
            Vehiculo v = inv.getArgument(0);
            v.setId(VEHICULO_ID);
            return v;
        });

        Vehiculo resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(resultado.getPatente()).isEqualTo("BGJK-91");
        verify(repositorio).save(any(Vehiculo.class));
    }

    @Test
    @DisplayName("crear genera qrCode automatico cuando el dto no lo incluye")
    void crear_sinQrCode_generaUnoAutomatico() {
        VehiculoDto dto = new VehiculoDto();
        dto.setPatente("XX-00");
        dto.setMarca("MAN");
        dto.setModelo("TGX");
        dto.setAnio(2023);
        dto.setTipo("CAMION");
        dto.setQrCode(null);

        when(repositorio.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        Vehiculo resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getQrCode()).isNotNull().isNotBlank();
    }

    // ─── actualizar ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("actualizar modifica solo los campos no nulos del dto")
    void actualizar_camposNoNulos_actualizaSolo() {
        when(repositorio.findByIdAndEmpresaId(VEHICULO_ID, EMPRESA_ID))
            .thenReturn(Optional.of(vehiculoEjemplo));
        when(repositorio.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        VehiculoDto dto = new VehiculoDto();
        dto.setEstado("EN_MANTENCION");
        // patente no se envia, debe quedar como esta

        Vehiculo resultado = servicio.actualizar(VEHICULO_ID, EMPRESA_ID, dto);

        assertThat(resultado.getEstado()).isEqualTo("EN_MANTENCION");
        assertThat(resultado.getPatente()).isEqualTo("BGJK-91"); // no cambio
    }

    @Test
    @DisplayName("actualizar lanza 404 cuando el vehiculo no pertenece a la empresa")
    void actualizar_empresaAjena_lanza404() {
        when(repositorio.findByIdAndEmpresaId(VEHICULO_ID, "EMP-999"))
            .thenReturn(Optional.empty());

        VehiculoDto dto = new VehiculoDto();
        dto.setEstado("BAJA");

        assertThatThrownBy(() -> servicio.actualizar(VEHICULO_ID, "EMP-999", dto))
            .isInstanceOf(ResponseStatusException.class);
    }

    // ─── eliminar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar marca el vehiculo como eliminado en vez de borrarlo fisicamente")
    void eliminar_vehiculoPropio_marcaEliminado() {
        when(repositorio.findByIdAndEmpresaId(VEHICULO_ID, EMPRESA_ID))
            .thenReturn(Optional.of(vehiculoEjemplo));
        when(repositorio.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio.eliminar(VEHICULO_ID, EMPRESA_ID);

        assertThat(vehiculoEjemplo.getEliminado()).isEqualTo(1);
        verify(repositorio).save(vehiculoEjemplo);
    }

    @Test
    @DisplayName("eliminar lanza 404 cuando el vehiculo pertenece a otra empresa")
    void eliminar_empresaAjena_lanza404() {
        when(repositorio.findByIdAndEmpresaId(VEHICULO_ID, "EMP-999"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(VEHICULO_ID, "EMP-999"))
            .isInstanceOf(ResponseStatusException.class);
        verify(repositorio, never()).save(any());
    }
}
