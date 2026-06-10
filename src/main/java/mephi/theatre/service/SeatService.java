package mephi.theatre.service;

import mephi.theatre.dto.SeatResponse;
import mephi.theatre.entity.Seat;
import mephi.theatre.enums.SeatStatus;
import mephi.theatre.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatService {

    private final SeatRepository seatRepository;

    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    public List<SeatResponse> getAllSeats() {
        return seatRepository.findAllByOrderByRowNumAscSeatNumAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private SeatResponse toResponse(Seat seat) {
        SeatResponse resp = new SeatResponse();
        resp.setId(seat.getId());
        resp.setRow(seat.getRowNum());
        resp.setNumber(seat.getSeatNum());
        resp.setStatus(seat.getStatus());
        return resp;
    }
}