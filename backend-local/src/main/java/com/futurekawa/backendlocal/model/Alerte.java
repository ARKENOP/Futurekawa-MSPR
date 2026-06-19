package com.futurekawa.backendlocal.model;

import java.io.Serializable;
import java.time.LocalDateTime;

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

import com.futurekawa.backendlocal.model.enums.NiveauAlerte;
import com.futurekawa.backendlocal.model.enums.StatutAlerte;
import com.futurekawa.backendlocal.model.enums.TypeAlerte;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alerte")
@Getter
@Setter
@NoArgsConstructor
public class Alerte implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerte")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_alerte", nullable = false, length = 50)
    private TypeAlerte typeAlerte;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", nullable = false, length = 20)
    private NiveauAlerte niveau;

    @Column(name = "date_heure_creation", nullable = false)
    private LocalDateTime dateHeureCreation;

    @Column(name = "message_description", nullable = false, columnDefinition = "TEXT")
    private String messageDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_alerte", nullable = false, length = 20)
    private StatutAlerte statutAlerte = StatutAlerte.OUVERTE;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesure_declencheuse_id")
    private MesureStockage mesureDeclencheuse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_concerne_id")
    private Lot lotConcerne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrepot_id", nullable = false)
    private Entrepot entrepot;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alerte)) return false;
        Alerte alerte = (Alerte) o;
        return id != null && id.equals(alerte.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
