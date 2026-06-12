package mephi.theatre.config;

import mephi.theatre.entity.Hall;
import mephi.theatre.entity.Seat;
import mephi.theatre.enums.SeatStatus;
import mephi.theatre.repository.HallRepository;
import mephi.theatre.repository.SeatRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SeatRepository seatRepository;
    private final HallRepository hallRepository;

    public DataInitializer(SeatRepository seatRepository, HallRepository hallRepository) {
        this.seatRepository = seatRepository;
        this.hallRepository = hallRepository;
    }

    @Override
    public void run(String... args) {
        // Проверяем, есть ли места
        if (seatRepository.count() == 0) {
            System.out.println("Создание зала и мест...");

            // Находим или создаём зал
            Hall hall = hallRepository.findById(1L).orElseGet(() -> {
                Hall newHall = new Hall();
                newHall.setName("Главный зал");
                newHall.setRows(10);
                newHall.setSeatsPerRow(20);
                return hallRepository.save(newHall);
            });

            // Создаём 200 мест
            for (int row = 1; row <= hall.getRows(); row++) {
                for (int seatNum = 1; seatNum <= hall.getSeatsPerRow(); seatNum++) {
                    Seat seat = new Seat();
                    seat.setRowNum(row);
                    seat.setSeatNum(seatNum);
                    seat.setStatus(SeatStatus.FREE);
                    seat.setHall(hall);
                    seatRepository.save(seat);
                }
            }

            System.out.println(" Создано мест: " + seatRepository.count());
        } else {
            System.out.println("Места уже есть. Количество мест: " + seatRepository.count());
        }
    }
}