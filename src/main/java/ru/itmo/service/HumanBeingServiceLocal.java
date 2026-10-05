package ru.itmo.service;

import jakarta.ejb.Local;
import ru.itmo.entity.Car;
import ru.itmo.entity.Coordinates;
import ru.itmo.entity.HumanBeing;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

@Local
public interface HumanBeingServiceLocal {
    HumanBeing saveHumanBeing(HumanBeing humanBeing);

    Optional<HumanBeing> findHumanBeingById(int id);

    List<HumanBeing> findAllHumanBeings();

    HumanBeing updateHumanBeing(HumanBeing humanBeing);

    boolean deleteHumanBeing(int id);

    Car saveCar(Car car);

    Optional<Car> findCarById(int id);

    List<Car> findAllCars();

    Car updateCar(Car car);

    boolean deleteCar(int id);

    Coordinates saveCoordinates(Coordinates coordinates);

    Optional<Coordinates> findCoordinatesById(int id);

    List<Coordinates> findAllCoordinates();

    Coordinates updateCoordinates(Coordinates coordinates);

    boolean deleteCoordinates(int id);

    OptionalDouble averageMinutesOfWaiting();

    Map<String, Long> countHumanBeingsByName();

    HumanBeing[] findHumanBeingsByNameSubstring(String substring);

    int deleteHeroesWithoutToothpicks();

    int makeHeroesGloomy();
}
