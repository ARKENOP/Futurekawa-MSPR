package com.futurekawa.backendlocal.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.backendlocal.model.enums.StatutLot;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long> {

    @EntityGraph(attributePaths = {"exploitation", "entrepot", "pays"})
    Page<Lot> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"exploitation", "entrepot", "pays"})
    Page<Lot> findByStatutLot(StatutLot statutLot, Pageable pageable);

    @EntityGraph(attributePaths = {"exploitation", "entrepot", "pays"})
    Page<Lot> findByEntrepotId(Long entrepotId, Pageable pageable);

    @EntityGraph(attributePaths = {"exploitation", "entrepot", "pays"})
    Page<Lot> findByEntrepotIdAndStatutLot(Long entrepotId, StatutLot statutLot, Pageable pageable);

    /**
     * Finds lots that entered storage before a certain date and are not yet marked as 'PERIME'.
     */
    @EntityGraph(attributePaths = {"exploitation", "entrepot", "pays"})
    @Query("SELECT l FROM Lot l WHERE l.dateEntreeStockage < :thresholdDate AND l.statutLot != :perimeStatus")
    List<Lot> findLotsOlderThanAndStatutNot(
            @Param("thresholdDate") LocalDateTime thresholdDate,
            @Param("perimeStatus") StatutLot perimeStatus
    );
}
