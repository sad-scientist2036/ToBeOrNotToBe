package mephi.theatre.repository;

import mephi.theatre.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findBySeatId(Long seatId);

    List<Ticket> findByUserId(Long userId);

    @Query("SELECT t FROM Ticket t WHERE t.user.email = :email")
    List<Ticket> findByUserEmail(@Param("email") String email);
}