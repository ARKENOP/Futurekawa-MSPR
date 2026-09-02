package com.futurekawa.backendlocal.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.futurekawa.backendlocal.model.Alerte;
import com.futurekawa.lib.enums.StatutAlerte;
import com.futurekawa.lib.enums.TypeAlerte;

@Repository
public interface AlerteRepository extends JpaRepository<Alerte, Long> {
    @EntityGraph(attributePaths = {"entrepot", "lotConcerne"})
    Page<Alerte> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"entrepot", "lotConcerne"})
    Page<Alerte> findByStatutAlerte(StatutAlerte statutAlerte, Pageable pageable);

    @EntityGraph(attributePaths = {"entrepot", "lotConcerne"})
    Page<Alerte> findByTypeAlerte(TypeAlerte typeAlerte, Pageable pageable);

    @EntityGraph(attributePaths = {"entrepot", "lotConcerne"})
    Page<Alerte> findByStatutAlerteAndTypeAlerte(StatutAlerte statutAlerte, TypeAlerte typeAlerte, Pageable pageable);

    @EntityGraph(attributePaths = {"entrepot", "lotConcerne"})
    Page<Alerte> findByEntrepotId(Long entrepotId, Pageable pageable);

    Optional<Alerte> findFirstByEntrepotIdAndTypeAlerteAndStatutAlerte(
            Long entrepotId, TypeAlerte typeAlerte, StatutAlerte statutAlerte);
}
