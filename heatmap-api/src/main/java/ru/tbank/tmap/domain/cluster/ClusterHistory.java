package ru.tbank.tmap.domain.cluster;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.tbank.tmap.domain.geo.H3Resolution;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "cluster_history")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ClusterHistory {

    @EmbeddedId
    @EqualsAndHashCode.Include
    @ToString.Include
    private Id id;

    @NotNull
    @PositiveOrZero
    @Column(name = "tx_count", nullable = false)
    @ToString.Include
    private Integer txCount;

    @NotNull
    @PositiveOrZero
    @Column(name = "avg_check", nullable = false, precision = 12, scale = 2)
    private BigDecimal avgCheck;

    @NotNull
    @PositiveOrZero
    @Column(name = "sum_amount", nullable = false, precision = 14, scale = 2)
    @ToString.Include
    private BigDecimal sumAmount;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ClusterHistory(Id id, int txCount, BigDecimal avgCheck, BigDecimal sumAmount) {
        this.id = Objects.requireNonNull(id, "id");
        this.txCount = txCount;
        this.avgCheck = Objects.requireNonNull(avgCheck, "avgCheck");
        this.sumAmount = Objects.requireNonNull(sumAmount, "sumAmount");
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    @SuppressWarnings("PMD.ShortClassName")
    public static class Id implements Serializable {

        @Column(name = "h3_index", nullable = false)
        private long h3Index;

        @Column(name = "resolution", nullable = false)
        private H3Resolution resolution;

        @Column(name = "category", nullable = false, length = 64)
        private String category;

        @Column(name = "hour_bucket", nullable = false)
        private OffsetDateTime hourBucket;
    }
}
