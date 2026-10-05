package ru.itmo.repository;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import ru.itmo.entity.Car;
import ru.itmo.entity.Coordinates;
import ru.itmo.entity.HumanBeing;
import ru.itmo.service.HumanBeingRepository;

import java.util.List;
import java.util.Optional;

@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class JpaHumanBeingRepository implements HumanBeingRepository {
    @PersistenceContext(unitName = "lab")
    private EntityManager entityManager;

    @Override
    public HumanBeing saveHumanBeing(HumanBeing humanBeing) {
        entityManager.persist(humanBeing);
        return humanBeing;
    }

    @Override
    public Optional<HumanBeing> findHumanBeingById(int id) {
        return Optional.ofNullable(entityManager.find(HumanBeing.class, id));
    }

    @Override
    public List<HumanBeing> findAllHumanBeings() {
        return entityManager.createQuery("SELECT h FROM HumanBeing h ORDER BY h.id", HumanBeing.class)
                .getResultList();
    }

    @Override
    public HumanBeing updateHumanBeing(HumanBeing humanBeing) {
        HumanBeing existing = entityManager.find(HumanBeing.class, humanBeing.getId());
        if (existing == null) {
            throw new EntityNotFoundException("HumanBeing с ID " + humanBeing.getId() + " не найден");
        }

        existing.setName(humanBeing.getName());
        existing.setCoordinates(humanBeing.getCoordinates());
        existing.setRealHero(humanBeing.isRealHero());
        existing.setHasToothpick(humanBeing.isHasToothpick());
        existing.setCar(humanBeing.getCar());
        existing.setMood(humanBeing.getMood());
        existing.setImpactSpeed(humanBeing.getImpactSpeed());
        existing.setMinutesOfWaiting(humanBeing.getMinutesOfWaiting());
        existing.setWeaponType(humanBeing.getWeaponType());
        return existing;
    }

    @Override
    public boolean deleteHumanBeing(int id) {
        HumanBeing humanBeing = entityManager.find(HumanBeing.class, id);
        if (humanBeing == null) {
            return false;
        }
        entityManager.remove(humanBeing);
        return true;
    }

    @Override
    public Car saveCar(Car car) {
        entityManager.persist(car);
        return car;
    }

    @Override
    public Optional<Car> findCarById(int id) {
        return Optional.ofNullable(entityManager.find(Car.class, id));
    }

    @Override
    public List<Car> findAllCars() {
        return entityManager.createQuery("SELECT c FROM Car c ORDER BY c.id", Car.class)
                .getResultList();
    }

    @Override
    public Car updateCar(Car car) {
        Car existing = entityManager.find(Car.class, car.getId());
        if (existing == null) {
            throw new EntityNotFoundException("Car с ID " + car.getId() + " не найден");
        }
        existing.setCool(car.isCool());
        return existing;
    }

    @Override
    public boolean deleteCar(int id) {
        Car car = entityManager.find(Car.class, id);
        if (car == null) {
            return false;
        }
        entityManager.remove(car);
        return true;
    }

    @Override
    public Coordinates saveCoordinates(Coordinates coordinates) {
        entityManager.persist(coordinates);
        return coordinates;
    }

    @Override
    public Optional<Coordinates> findCoordinatesById(int id) {
        return Optional.ofNullable(entityManager.find(Coordinates.class, id));
    }

    @Override
    public List<Coordinates> findAllCoordinates() {
        return entityManager.createQuery("SELECT c FROM Coordinates c ORDER BY c.id", Coordinates.class)
                .getResultList();
    }

    @Override
    public Coordinates updateCoordinates(Coordinates coordinates) {
        Coordinates existing = entityManager.find(Coordinates.class, coordinates.getId());
        if (existing == null) {
            throw new EntityNotFoundException("Coordinates с ID " + coordinates.getId() + " не найден");
        }
        existing.setX(coordinates.getX());
        existing.setY(coordinates.getY());
        return existing;
    }

    @Override
    public boolean deleteCoordinates(int id) {
        Coordinates coordinates = entityManager.find(Coordinates.class, id);
        if (coordinates == null) {
            return false;
        }
        entityManager.remove(coordinates);
        return true;
    }
}
