package com.futurekawa.backendlocal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.futurekawa.backendlocal.config.PaysProperties;
import com.futurekawa.backendlocal.exception.ResourceNotFoundException;
import com.futurekawa.backendlocal.mapper.EntrepotMapper;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.Pays;
import com.futurekawa.backendlocal.repository.EntrepotRepository;
import com.futurekawa.backendlocal.repository.PaysRepository;
import com.futurekawa.lib.dto.response.EntrepotResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntrepotServiceTest {
    @Mock private EntrepotRepository entrepotRepository;
    @Mock private PaysRepository paysRepository;
    @Mock private EntrepotMapper entrepotMapper;
    @Mock private PaysProperties paysProperties;

    private EntrepotService service;

    private static final EntrepotResponse RESPONSE =
            new EntrepotResponse(1L, "E", "loc", 100, "actif", 1L, 1L);

    @BeforeEach
    void setUp() {
        service = new EntrepotService(entrepotRepository, paysRepository, entrepotMapper, paysProperties);
        when(paysProperties.code()).thenReturn("BR");
        when(entrepotMapper.toResponse(org.mockito.ArgumentMatchers.any())).thenReturn(RESPONSE);
    }

    @Test
    void listAllResolvesCountryThenLists() {
        Pays pays = new Pays();
        pays.setId(5L);
        when(paysRepository.findByCodePays("BR")).thenReturn(Optional.of(pays));
        when(entrepotRepository.findByPaysId(5L)).thenReturn(List.of(new Entrepot()));

        assertThat(service.listAll()).containsExactly(RESPONSE);
    }

    @Test
    void listAllThrowsWhenCountryMissing() {
        when(paysRepository.findByCodePays("BR")).thenReturn(Optional.empty());
        assertThatThrownBy(service::listAll).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listByExploitationDelegatesToRepository() {
        when(entrepotRepository.findByExploitationId(9L)).thenReturn(List.of(new Entrepot()));
        assertThat(service.listByExploitation(9L)).containsExactly(RESPONSE);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(entrepotRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdMapsWhenPresent() {
        when(entrepotRepository.findById(1L)).thenReturn(Optional.of(new Entrepot()));
        assertThat(service.getById(1L)).isEqualTo(RESPONSE);
    }
}
