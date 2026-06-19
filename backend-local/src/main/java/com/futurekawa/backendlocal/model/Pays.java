package com.futurekawa.backendlocal.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pays")
@Getter
@Setter
@NoArgsConstructor // Required by JPA
public class Pays implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pays")
    private Long id;

    @Column(name = "code_pays", nullable = false, unique = true, length = 5)
    private String codePays;

    @Column(name = "nom_pays", nullable = false, length = 100)
    private String nomPays;

    @Column(name = "temperature_ideale_c", nullable = false, precision = 4, scale = 1)
    private BigDecimal temperatureIdealeC;

    @Column(name = "humidite_ideale_pourcent", nullable = false, precision = 4, scale = 1)
    private BigDecimal humiditeIdealePourcent;

    @Column(name = "tolerance_temperature_c", nullable = false, precision = 4, scale = 1)
    private BigDecimal toleranceTemperatureC;

    @Column(name = "tolerance_humidite_pourcent", nullable = false, precision = 4, scale = 1)
    private BigDecimal toleranceHumiditePourcent;

    @Column(name = "est_actif", nullable = false)
    private Boolean estActif = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pays)) return false;
        Pays pays = (Pays) o;
        return codePays != null && codePays.equals(pays.getCodePays());
    }

    @Override
    public int hashCode() {
        return Objects.hash(codePays);
    }
}
