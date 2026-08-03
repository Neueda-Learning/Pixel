package com.pixel.portfolio.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "holdings", uniqueConstraints = @UniqueConstraint(columnNames = { "portfolio_id", "symbol" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holding {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "portfolio_id", nullable = false)
	private Portfolio portfolio;

	@Column(nullable = false, length = 32)
	private String symbol;

	@Column(nullable = false, precision = 19, scale = 6)
	@Builder.Default
	private BigDecimal quantity = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal averagePrice;

	@PrePersist
	@PreUpdate
	public void normalize() {
		if (quantity == null) {
			quantity = BigDecimal.ZERO;
		}
		if (averagePrice == null) {
			averagePrice = BigDecimal.ZERO;
		}
		quantity = quantity.setScale(6, RoundingMode.HALF_UP);
		averagePrice = averagePrice.setScale(4, RoundingMode.HALF_UP);
		symbol = symbol == null ? null : symbol.trim().toUpperCase();
	}
}

