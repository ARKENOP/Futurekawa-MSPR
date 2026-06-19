package com.futurekawa.backendlocal.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@Table(name = "mesure_stockage")
@Getter
@Setter
@NoArgsConstructor
public class MesureStockage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mesure")
    private Long id;

    @Column(name = "id_capteur", nullable = false, length = 100)
    private String idCapteur;

    @Column(name = "date_heure_mesure", nullable = false)
    private LocalDateTime dateHeureMesure;

    @Column(name = "temperature_c", nullable = false, precision = 4, scale = 1)
    private BigDecimal temperatureC;

    @Column(name = "humidite_pourcent", nullable = false, precision = 4, scale = 1)
    private BigDecimal humiditePourcent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrepot_id", nullable = false)
    private Entrepot entrepot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MesureStockage)) return false;
        MesureStockage that = (MesureStockage) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
