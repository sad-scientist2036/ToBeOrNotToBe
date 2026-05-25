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

    // Шаг 1: Временное резервирование (когда пользователь начал бронировать)
    @Transactional
    public Seat holdSeat(Long seatId) {
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new RuntimeException("Место не найдено"));

        if (seat.getStatus() != SeatStatus.FREE) {
            throw new RuntimeException("Место уже не доступно");
        }

        seat.setStatus(SeatStatus.HOLD);
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5)); // блокировка на 5 минут
        return seatRepository.save(seat);
    }

    // Шаг 2: Подтверждение бронирования (после ввода данных)
    @Transactional
    public Ticket confirmBooking(BookRequest request) {
        Seat seat = seatRepository.findByIdWithLock(request.getSeatId())
                .orElseThrow(() -> new RuntimeException("Место не найдено"));

        // Проверяем, что место в статусе HOLD и не истекло время
        if (seat.getStatus() != SeatStatus.HOLD) {
            throw new RuntimeException("Место не забронировано. Возможно, время вышло.");
        }

        if (seat.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            seat.setStatus(SeatStatus.FREE);
            seat.setHoldExpiresAt(null);
            seatRepository.save(seat);
            throw new RuntimeException("Время бронирования истекло");
        }

        seat.setStatus(SeatStatus.BOOKED);
        seat.setHoldExpiresAt(null);
        seatRepository.save(seat);

        Ticket ticket = new Ticket();
        ticket.setSeat(seat);
        ticket.setCustomerName(request.getCustomerName());
        ticket.setCustomerPhone(request.getCustomerPhone());
        ticket.setBookedAt(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }

    // Отмена временного резервирования
    @Transactional
    public void cancelHold(Long seatId) {
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new RuntimeException("Место не найдено"));

        if (seat.getStatus() == SeatStatus.HOLD) {
            seat.setStatus(SeatStatus.FREE);
            seat.setHoldExpiresAt(null);
            seatRepository.save(seat);
        }
    }

    @Transactional
    public void releaseSeat(Long seatId) {
        Ticket ticket = ticketRepository.findBySeatId(seatId)
                .orElseThrow(() -> new RuntimeException("Бронирование не найдено"));

        Seat seat = ticket.getSeat();
        seat.setStatus(SeatStatus.FREE);
        seat.setHoldExpiresAt(null);
        seatRepository.save(seat);

        ticketRepository.delete(ticket);
    }
}