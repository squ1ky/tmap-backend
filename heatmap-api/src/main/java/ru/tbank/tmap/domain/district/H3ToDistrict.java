package ru.tbank.tmap.domain.district;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.tbank.tmap.domain.geo.H3Resolution;

import java.util.Objects;

@Entity
@Table(name = "h3_to_district")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class H3ToDistrict {

    @Id
    @Column(name = "h3_index", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private long h3Index;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(name = "resolution", nullable = false)
    @ToString.Include
    private H3Resolution resolution;

    public H3ToDistrict(long h3Index, District district, H3Resolution resolution) {
        this.h3Index = h3Index;
        this.district = Objects.requireNonNull(district, "district");
        this.resolution = resolution;
    }
}
