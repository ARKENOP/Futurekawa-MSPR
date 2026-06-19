package com.futurekawa.backendlocal.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entrepot")
@Getter
@Setter
@NoArgsConstructor
public class Entrepot implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrepot")
    private Long id;

    @Column(name = "nom_entrepot", nullable = false, length = 200)
    private String nomEntrepot;

    @Column(name = "localisation", length = 300)
    private String localisation;

    @Column(name = "capacite_max")
    private Integer capaciteMax;

    @Column(name = "statut_entrepot", nullable = false, length = 50)
    private String statutEntrepot = "actif";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exploitation_id", nullable = false)
    private Exploitation exploitation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pays_id", nullable = false)
    private Pays pays;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entrepot)) return false;
        Entrepot entrepot = (Entrepot) o;
        return id != null && id.equals(entrepot.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
