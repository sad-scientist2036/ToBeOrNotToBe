package mephi.theatre.repository;

import mephi.theatre.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findBySeatId(Long seatId);
}