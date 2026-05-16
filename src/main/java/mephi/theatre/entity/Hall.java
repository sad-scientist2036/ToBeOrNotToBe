package mephi.theatre.entity;
import jakarta.persistence.*;
@Entity
@Table(name = "halls")

public class Hall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int rows;

    private int seatsPerRow;
}