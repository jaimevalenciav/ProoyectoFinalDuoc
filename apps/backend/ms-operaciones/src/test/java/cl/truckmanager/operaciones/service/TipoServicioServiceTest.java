package cl.truckmanager.operaciones.service;

import cl.truckmanager.operaciones.dto.TipoServicioDto;
import cl.truckmanager.operaciones.entity.TipoServicio;
import cl.truckmanager.operaciones.repository.TipoServicioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TipoServicioService — pruebas unitarias")
class TipoServicioServiceTest {

    @Mock
    private TipoServicioRepository repositorio;

    @InjectMocks
    private TipoServicioService servicio;

    private static final String EMPRESA_ID = "EMP-001";

    private TipoServicio tipoEjemplo;

    @BeforeEach
    void configurar() {
        tipoEjemplo = TipoServicio.builder()
            .id("TS-001")
            .empresaId(EMPRESA_ID)
            .codigo("TRANS")
            .nombre("Transporte General")
            .activo(1)
            .build();
    }

    @Test
    @DisplayName("getAll retorna solo tipos activos de la empresa")
    void getAll_retornaTiposActivos() {
        when(repositorio.findByEmpresaIdAndActivo(EMPRESA_ID, 1))
            .thenReturn(List.of(tipoEjemplo));

        List<TipoServicio> resultado = servicio.getAll(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getActivo()).isEqualTo(1);
    }

    @Test
    @DisplayName("getAllIncluidos retorna todos los tipos sin importar activo")
    void getAllIncluidos_retornaTodos() {
        when(repositorio.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(tipoEjemplo));

        List<TipoServicio> resultado = servicio.getAllIncluidos(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
        verify(repositorio).findByEmpresaId(EMPRESA_ID);
    }

    @Test
    @DisplayName("crear persiste tipo de servicio con activo=1 por defecto")
    void crear_sinActivo_asignaUno() {
        when(repositorio.save(any(TipoServicio.class))).thenReturn(tipoEjemplo);

        TipoServicioDto dto = new TipoServicioDto();
        dto.setCodigo("TRANS");
        dto.setNombre("Transporte General");

        TipoServicio resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(resultado.getActivo()).isEqualTo(1);
        verify(repositorio).save(any(TipoServicio.class));
    }

    @Test
    @DisplayName("actualizar modifica codigo y nombre del tipo de servicio")
    void actualizar_camposValidos_modifica() {
        when(repositorio.findById("TS-001")).thenReturn(Optional.of(tipoEjemplo));
        when(repositorio.save(any(TipoServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoServicioDto dto = new TipoServicioDto();
        dto.setNombre("Transporte Refrigerado");
        dto.setActivo(0);

        TipoServicio resultado = servicio.actualizar("TS-001", dto);

        assertThat(resultado.getNombre()).isEqualTo("Transporte Refrigerado");
        assertThat(resultado.getActivo()).isEqualTo(0);
    }

    @Test
    @DisplayName("actualizar lanza EntityNotFoundException cuando no existe")
    void actualizar_inexistente_lanzaExcepcion() {
        when(repositorio.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar("X", new TipoServicioDto()))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
