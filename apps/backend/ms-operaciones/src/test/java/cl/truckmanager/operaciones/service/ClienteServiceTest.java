package cl.truckmanager.operaciones.service;

import cl.truckmanager.operaciones.dto.ClienteDto;
import cl.truckmanager.operaciones.entity.Cliente;
import cl.truckmanager.operaciones.repository.ClienteRepository;
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
@DisplayName("ClienteService — pruebas unitarias")
class ClienteServiceTest {

    @Mock
    private ClienteRepository repositorio;

    @InjectMocks
    private ClienteService servicio;

    private static final String EMPRESA_ID  = "EMP-001";
    private static final String CLIENTE_ID  = "CLI-001";

    private Cliente clienteEjemplo;

    @BeforeEach
    void configurar() {
        clienteEjemplo = Cliente.builder()
            .id(CLIENTE_ID)
            .empresaId(EMPRESA_ID)
            .rut("76543210-K")
            .razonSocial("Logística Chile SpA")
            .giro("Transporte de carga")
            .ciudad("Santiago")
            .activo(1)
            .build();
    }

    @Test
    @DisplayName("getAll devuelve pagina filtrada por empresa")
    void getAll_devuelveResultados() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), eq(1), isNull(), any()))
            .thenReturn(new PageImpl<>(List.of(clienteEjemplo)));

        Page<Cliente> resultado = servicio.getAll(EMPRESA_ID, null, 0, 20);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("getAll con busqueda en blanco la convierte a null")
    void getAll_busquedaEnBlanco_pasaNull() {
        when(repositorio.buscarPorFiltros(eq(EMPRESA_ID), eq(1), isNull(), any()))
            .thenReturn(Page.empty());

        servicio.getAll(EMPRESA_ID, "  ", 0, 20);

        verify(repositorio).buscarPorFiltros(eq(EMPRESA_ID), eq(1), isNull(), any());
    }

    @Test
    @DisplayName("getAllActivos retorna solo clientes activos")
    void getAllActivos_retornaActivos() {
        when(repositorio.findByEmpresaIdAndActivo(EMPRESA_ID, 1))
            .thenReturn(List.of(clienteEjemplo));

        List<Cliente> resultado = servicio.getAllActivos(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("getById retorna cliente cuando existe")
    void getById_existente_retornaCliente() {
        when(repositorio.findById(CLIENTE_ID)).thenReturn(Optional.of(clienteEjemplo));

        Cliente resultado = servicio.getById(CLIENTE_ID);

        assertThat(resultado.getRazonSocial()).isEqualTo("Logística Chile SpA");
    }

    @Test
    @DisplayName("getById lanza EntityNotFoundException cuando no existe")
    void getById_inexistente_lanzaExcepcion() {
        when(repositorio.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.getById("X"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("crear persiste cliente con activo=1 por defecto")
    void crear_sinActivo_asignaUno() {
        when(repositorio.save(any(Cliente.class))).thenReturn(clienteEjemplo);

        ClienteDto dto = new ClienteDto();
        dto.setRut("76543210-K");
        dto.setRazonSocial("Logística Chile SpA");

        Cliente resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(resultado.getActivo()).isEqualTo(1);
    }

    @Test
    @DisplayName("actualizar modifica campos seleccionados del cliente")
    void actualizar_camposSeleccionados_actualiza() {
        when(repositorio.findById(CLIENTE_ID)).thenReturn(Optional.of(clienteEjemplo));
        when(repositorio.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteDto dto = new ClienteDto();
        dto.setGiro("Transporte refrigerado");
        dto.setCiudad("Valparaiso");

        Cliente resultado = servicio.actualizar(CLIENTE_ID, dto);

        assertThat(resultado.getGiro()).isEqualTo("Transporte refrigerado");
        assertThat(resultado.getCiudad()).isEqualTo("Valparaiso");
        assertThat(resultado.getRazonSocial()).isEqualTo("Logística Chile SpA"); // no cambio
    }
}
