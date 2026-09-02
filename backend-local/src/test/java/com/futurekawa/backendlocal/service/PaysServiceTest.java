package com.futurekawa.backendlocal.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.futurekawa.backendlocal.mapper.PaysMapper;
import com.futurekawa.backendlocal.model.Pays;
import com.futurekawa.backendlocal.repository.PaysRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaysServiceTest {
    @Mock private PaysRepository paysRepository;
    @Mock private PaysMapper paysMapper;
    @Mock private PaysProperties paysProperties;

    private PaysService service;

    @BeforeEach
    void setUp() {
        service = new PaysService(paysRepository, paysMapper, paysProperties);
        when(paysProperties.code()).thenReturn("BR");
    }

    @Test
    void getCountryInfoMapsWhenSeeded() {
        Pays pays = new Pays();
        var response = new com.futurekawa.lib.dto.response.PaysResponse(
                1L, "BR", "Brésil", null, null, null, null, true);
        when(paysRepository.findByCodePays("BR")).thenReturn(Optional.of(pays));
        when(paysMapper.toResponse(pays)).thenReturn(response);

        service.getCountryInfo();

        verify(paysMapper).toResponse(pays);
    }

    @Test
    void getCountryInfoThrowsWhenNotInitialized() {
        when(paysRepository.findByCodePays("BR")).thenReturn(Optional.empty());
        assertThatThrownBy(service::getCountryInfo).isInstanceOf(ResourceNotFoundException.class);
    }
}
