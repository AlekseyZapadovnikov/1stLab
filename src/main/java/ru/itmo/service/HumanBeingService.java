package ru.itmo.service;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import ru.itmo.entity.Car;
import ru.itmo.entity.Coordinates;
import ru.itmo.entity.HumanBeing;
import ru.itmo.entity.Mood;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class HumanBeingService implements HumanBeingServiceLocal {
    @EJB
    private HumanBeingRepository repository;

    public HumanBeing saveHumanBeing(HumanBeing humanBeing) {
        validateHumanBeing(humanBeing);
        resolveRelations(humanBeing);
        return repository.saveHumanBeing(humanBeing);
    }

    public Optional<HumanBeing> findHumanBeingById(int id) {
        return repository.findHumanBeingById(id);
    }

    public List<HumanBeing> findAllHumanBeings() {
        return repository.findAllHumanBeings();
    }

    public HumanBeing updateHumanBeing(HumanBeing humanBeing) {
        validateHumanBeing(humanBeing);
        resolveRelations(humanBeing);
        return repository.updateHumanBeing(humanBeing);
    }

    public boolean deleteHumanBeing(int id) {
        return repository.deleteHumanBeing(id);
    }

    public Car saveCar(Car car) {
        return repository.saveCar(car);
    }

    public Optional<Car> findCarById(int id) {
        return repository.findCarById(id);
    }

    public List<Car> findAllCars() {
        return repository.findAllCars();
    }

    public Car updateCar(Car car) {
        return repository.updateCar(car);
    }

    public boolean deleteCar(int id) {
        boolean referenced = repository.findAllHumanBeings().stream()
                .anyMatch(humanBeing -> humanBeing.getCar().getId() == id);
        if (referenced) {
            throw new IllegalStateException("Нельзя удалить машину с ID " + id + ": она связана с человеком");
        }
        return repository.deleteCar(id);
    }

    public Coordinates saveCoordinates(Coordinates coordinates) {
        validateCoordinates(coordinates);
        return repository.saveCoordinates(coordinates);
    }

    public Optional<Coordinates> findCoordinatesById(int id) {
        return repository.findCoordinatesById(id);
    }

    public List<Coordinates> findAllCoordinates() {
        return repository.findAllCoordinates();
    }

    public Coordinates updateCoordinates(Coordinates coordinates) {
        validateCoordinates(coordinates);
        return repository.updateCoordinates(coordinates);
    }

    public boolean deleteCoordinates(int id) {
        boolean referenced = repository.findAllHumanBeings().stream()
                .anyMatch(humanBeing -> humanBeing.getCoordinates().getId() == id);
        if (referenced) {
            throw new IllegalStateException("Нельзя удалить координаты с ID " + id + ": они связаны с человеком");
        }
        return repository.deleteCoordinates(id);
    }

    public OptionalDouble averageMinutesOfWaiting() {
        List<HumanBeing> humans = repository.findAllHumanBeings();
        if (humans.isEmpty()) {
            return OptionalDouble.empty();
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (HumanBeing human : humans) {
            sum = sum.add(BigDecimal.valueOf(human.getMinutesOfWaiting()));
        }
        return OptionalDouble.of(sum.divide(BigDecimal.valueOf(humans.size()), MathContext.DECIMAL128)
                .doubleValue());
    }

    public Map<String, Long> countHumanBeingsByName() {
        return repository.findAllHumanBeings().stream()
                .collect(Collectors.groupingBy(HumanBeing::getName, TreeMap::new, Collectors.counting()));
    }

    public HumanBeing[] findHumanBeingsByNameSubstring(String substring) {
        Objects.requireNonNull(substring, "Подстрока для поиска обязательна");
        return repository.findAllHumanBeings().stream()
                .filter(humanBeing -> humanBeing.getName().contains(substring))
                .toArray(HumanBeing[]::new);
    }

    public int deleteHeroesWithoutToothpicks() {
        int deleted = 0;
        for (HumanBeing humanBeing : repository.findAllHumanBeings()) {
            if (humanBeing.isRealHero() && !humanBeing.isHasToothpick()
                    && repository.deleteHumanBeing(humanBeing.getId())) {
                deleted++;
            }
        }
        return deleted;
    }

    public int makeHeroesGloomy() {
        int updated = 0;
        for (HumanBeing humanBeing : repository.findAllHumanBeings()) {
            if (humanBeing.isRealHero() && humanBeing.getMood() != Mood.GLOOM) {
                humanBeing.setMood(Mood.GLOOM);
                repository.updateHumanBeing(humanBeing);
                updated++;
            }
        }
        return updated;
    }

    private void validateHumanBeing(HumanBeing humanBeing) {
        if (humanBeing == null) {
            throw new IllegalArgumentException("Данные человека обязательны");
        }
        if (humanBeing.getName() == null || humanBeing.getName().isEmpty()) {
            throw new IllegalArgumentException("Имя обязательно и не может быть пустым");
        }
        if (humanBeing.getCar() == null) {
            throw new IllegalArgumentException("Машина обязательна");
        }
        validateCoordinates(humanBeing.getCoordinates());
    }

    private void validateCoordinates(Coordinates coordinates) {
        if (coordinates == null || coordinates.getX() == null || coordinates.getY() == null) {
            throw new IllegalArgumentException("Координаты x и y обязательны");
        }
        if (!(coordinates.getY() <= 791)) {
            throw new IllegalArgumentException("Координата y должна быть не больше 791");
        }
    }

    private void resolveRelations(HumanBeing humanBeing) {
        Car car = Objects.requireNonNull(humanBeing.getCar(), "Машина обязательна");
        Coordinates coordinates = Objects.requireNonNull(humanBeing.getCoordinates(), "Координаты обязательны");

        if (car.getId() == 0) {
            humanBeing.setCar(repository.saveCar(car));
        } else {
            humanBeing.setCar(repository.findCarById(car.getId()).orElseThrow(() ->
                    new IllegalArgumentException("Машина с ID " + car.getId() + " не найдена")));
        }

        if (coordinates.getId() == 0) {
            humanBeing.setCoordinates(repository.saveCoordinates(coordinates));
        } else {
            humanBeing.setCoordinates(repository.findCoordinatesById(coordinates.getId()).orElseThrow(() ->
                    new IllegalArgumentException("Координаты с ID " + coordinates.getId() + " не найдены")));
        }
    }
}
