package mephi.theatre.config;

import mephi.theatre.entity.Seat;
import mephi.theatre.enums.SeatStatus;
import mephi.theatre.repository.SeatRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SeatRepository seatRepository;

    public DataInitializer(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Override
    public void run(String... args) {
        if (seatRepository.count() == 0) {
            for (int row = 1; row <= 10; row++) {
                for (int seatNum = 1; seatNum <= 20; seatNum++) {
                    Seat seat = new Seat();
                    seat.setRowNum(row);
                    seat.setSeatNum(seatNum);
                    seat.setStatus(SeatStatus.FREE);
                    seatRepository.save(seat);
                }
            }
            System.out.println("Зал создан: 10 рядов по 20 мест");
        }
    }
}