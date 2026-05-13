package com.market.prediction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.market.prediction.entity.Pair;
import java.util.Optional;

public interface PairRepository extends JpaRepository<Pair, Long> {
    Optional<Pair> findBySymbol(String symbol);
}
