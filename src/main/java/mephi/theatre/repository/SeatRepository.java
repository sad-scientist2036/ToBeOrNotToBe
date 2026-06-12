package mephi.theatre.repository;

import mephi.theatre.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findAllByOrderByRowNumAscSeatNumAsc();

    @Query(value = "SELECT * FROM seats WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Seat> findByIdWithLock(@Param("id") Long id);
}