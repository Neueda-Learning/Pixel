package com.pixel.portfolio.repository;

import com.pixel.portfolio.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentRepository extends JpaRepository<Instrument, String> {
}
