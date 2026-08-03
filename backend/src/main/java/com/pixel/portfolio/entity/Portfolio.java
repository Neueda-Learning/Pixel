package com.pixel.portfolio.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "portfolios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String name;

	@Column(nullable = false, precision = 19, scale = 4)
	@Builder.Default
	private BigDecimal cashBalance = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

	@OneToMany(mappedBy = "portfolio", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Builder.Default
	private List<Holding> holdings = new ArrayList<>();

	@OneToMany(mappedBy = "portfolio", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Trade> trades = new ArrayList<>();

	@PrePersist
	@PreUpdate
	public void normalize() {
		if (cashBalance == null) {
			cashBalance = BigDecimal.ZERO;
		}
		cashBalance = cashBalance.setScale(4, RoundingMode.HALF_UP);
	}
}

