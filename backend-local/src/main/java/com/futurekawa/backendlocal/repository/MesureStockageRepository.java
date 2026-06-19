package com.futurekawa.backendlocal.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.futurekawa.backendlocal.model.MesureStockage;

@Repository
public interface MesureStockageRepository extends JpaRepository<MesureStockage, Long> {

    @EntityGraph(attributePaths = {"entrepot", "lot"})
    Page<MesureStockage> findByEntrepotIdOrderByDateHeureMesureDesc(Long entrepotId, Pageable pageable);

    @EntityGraph(attributePaths = {"entrepot", "lot"})
    Page<MesureStockage> findByEntrepotIdAndDateHeureMesureBetweenOrderByDateHeureMesureDesc(
            Long entrepotId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    @EntityGraph(attributePaths = {"entrepot", "lot"})
    Optional<MesureStockage> findTopByEntrepotIdOrderByDateHeureMesureDesc(Long entrepotId);
}
