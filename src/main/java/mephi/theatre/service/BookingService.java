package mephi.theatre.service;

import mephi.theatre.dto.BookRequest;
import mephi.theatre.entity.Seat;
import mephi.theatre.entity.Ticket;
import mephi.theatre.enums.SeatStatus;
import mephi.theatre.repository.SeatRepository;
import mephi.theatre.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class BookingService {

    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;

    public BookingService(SeatRepository seatRepository, TicketRepository ticketRepository) {
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public Ticket bookSeat(BookRequest request) {
        Seat seat = seatRepository.findByIdWithLock(request.getSeatId())
                .orElseThrow(() -> new RuntimeException("Место не найдено"));

        if (seat.getStatus() != SeatStatus.FREE) {
            throw new RuntimeException("Место уже занято");
        }

        seat.setStatus(SeatStatus.BOOKED);
        seatRepository.save(seat);

        Ticket ticket = new Ticket();
        ticket.setSeat(seat);
        ticket.setCustomerName(request.getCustomerName());
        ticket.setCustomerPhone(request.getCustomerPhone());
        ticket.setBookedAt(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }

    @Transactional
    public void releaseSeat(Long seatId) {
        Ticket ticket = ticketRepository.findBySeatId(seatId)
                .orElseThrow(() -> new RuntimeException("Бронирование не найдено"));

        Seat seat = ticket.getSeat();
        seat.setStatus(SeatStatus.FREE);
        seatRepository.save(seat);

        ticketRepository.delete(ticket);
    }
}