package com.futurekawa.backendlocal.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futurekawa.backendlocal.exception.ResourceNotFoundException;
import com.futurekawa.backendlocal.mapper.LotMapper;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.Exploitation;
import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.backendlocal.repository.EntrepotRepository;
import com.futurekawa.backendlocal.repository.ExploitationRepository;
import com.futurekawa.backendlocal.repository.LotRepository;
import com.futurekawa.lib.dto.request.CreateLotRequest;
import com.futurekawa.lib.dto.request.UpdateLotRequest;
import com.futurekawa.lib.dto.response.LotResponse;
import com.futurekawa.lib.enums.StatutLot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LotService {
    private final LotRepository lotRepository;
    private final ExploitationRepository exploitationRepository;
    private final EntrepotRepository entrepotRepository;
    private final LotMapper lotMapper;

    public Page<LotResponse> listAll(StatutLot statutLot, Pageable pageable) {
        Pageable effective = withDefaultSort(pageable, Sort.by(Sort.Direction.ASC, "dateEntreeStockage"));
        Page<Lot> lots = statutLot != null
                ? lotRepository.findByStatutLot(statutLot, effective)
                : lotRepository.findAll(effective);
        return lots.map(lotMapper::toResponse);
    }

    private static Pageable withDefaultSort(Pageable pageable, Sort fallback) {
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), fallback);
    }

    public LotResponse getById(Long id) {
        return lotRepository.findById(id)
                .map(lotMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found with ID: " + id));
    }

    @Transactional
    public LotResponse createLot(CreateLotRequest request) {
        Exploitation exploitation = exploitationRepository.findById(request.exploitationId())
                .orElseThrow(() -> new ResourceNotFoundException("Exploitation not found"));
        Entrepot entrepot = entrepotRepository.findById(request.entrepotId())
                .orElseThrow(() -> new ResourceNotFoundException("Entrepot not found"));

        Lot lot = lotMapper.toEntity(request);
        lot.setPays(entrepot.getPays());
        lot.setExploitation(exploitation);
        lot.setEntrepot(entrepot);
        lot.setDateEntreeStockage(request.dateEntreeStockage().atStartOfDay());

        Lot saved = lotRepository.save(lot);
        return lotMapper.toResponse(saved);
    }

    @Transactional
    public LotResponse updateStatut(Long id, UpdateLotRequest request) {
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found with ID: " + id));

        lot.setStatutLot(request.statutLot());
        Lot saved = lotRepository.save(lot);
        return lotMapper.toResponse(saved);
    }
}
