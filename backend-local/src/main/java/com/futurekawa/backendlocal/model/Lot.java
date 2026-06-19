package com.futurekawa.backendlocal.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.futurekawa.backendlocal.model.enums.StatutLot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lot")
@Getter
@Setter
@NoArgsConstructor
public class Lot implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lot")
    private Long id;

    @Column(name = "reference_lot", nullable = false, unique = true, length = 100)
    private String referenceLot;

    @Column(name = "date_entree_stockage", nullable = false)
    private LocalDateTime dateEntreeStockage;

    @Column(name = "date_recolte")
    private LocalDate dateRecolte;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_lot", nullable = false, length = 20)
    private StatutLot statutLot = StatutLot.CONFORME;

    @Column(name = "qualite_lot", length = 100)
    private String qualiteLot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exploitation_id", nullable = false)
    private Exploitation exploitation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrepot_id", nullable = false)
    private Entrepot entrepot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pays_id", nullable = false)
    private Pays pays;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lot)) return false;
        Lot lot = (Lot) o;
        return referenceLot != null && referenceLot.equals(lot.getReferenceLot());
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceLot);
    }
}
