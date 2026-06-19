package com.futurekawa.backendlocal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.futurekawa.backendlocal.model.Entrepot;

@Repository
public interface EntrepotRepository extends JpaRepository<Entrepot, Long> {

    @EntityGraph(attributePaths = {"exploitation", "pays"})
    List<Entrepot> findByExploitationId(Long exploitationId);

    @EntityGraph(attributePaths = {"exploitation", "pays"})
    List<Entrepot> findByPaysId(Long paysId);
}
