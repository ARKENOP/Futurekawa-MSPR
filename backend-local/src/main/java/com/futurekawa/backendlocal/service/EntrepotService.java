package com.futurekawa.backendlocal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futurekawa.backendlocal.config.PaysProperties;
import com.futurekawa.backendlocal.dto.response.EntrepotResponse;
import com.futurekawa.backendlocal.exception.ResourceNotFoundException;
import com.futurekawa.backendlocal.mapper.EntrepotMapper;
import com.futurekawa.backendlocal.model.Pays;
import com.futurekawa.backendlocal.repository.EntrepotRepository;
import com.futurekawa.backendlocal.repository.PaysRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntrepotService {

    private final EntrepotRepository entrepotRepository;
    private final PaysRepository paysRepository;
    private final EntrepotMapper entrepotMapper;
    private final PaysProperties paysProperties;

    public List<EntrepotResponse> listAll() {
        Pays pays = paysRepository.findByCodePays(paysProperties.code())
                .orElseThrow(() -> new ResourceNotFoundException("Pays not found"));

        return entrepotRepository.findByPaysId(pays.getId()).stream()
                .map(entrepotMapper::toResponse)
                .toList();
    }

    public List<EntrepotResponse> listByExploitation(Long exploitationId) {
        return entrepotRepository.findByExploitationId(exploitationId).stream()
                .map(entrepotMapper::toResponse)
                .toList();
    }

    public EntrepotResponse getById(Long id) {
        return entrepotRepository.findById(id)
                .map(entrepotMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Entrepot not found with ID: " + id));
    }
}
