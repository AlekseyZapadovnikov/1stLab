package ru.itmo.web;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import ru.itmo.entity.Car;
import ru.itmo.entity.Coordinates;
import ru.itmo.entity.HumanBeing;

final class EntityJson {
    private EntityJson() {
    }

    static JsonObject car(Car car) {
        return Json.createObjectBuilder().add("id", car.getId()).add("cool", car.isCool()).build();
    }

    static JsonObject coordinates(Coordinates coordinates) {
        return Json.createObjectBuilder().add("id", coordinates.getId())
                // Long values are strings to preserve precision in JavaScript.
                .add("x", coordinates.getX().toString()).add("y", coordinates.getY()).build();
    }

    static JsonObject human(HumanBeing human) {
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("id", human.getId()).add("name", human.getName())
                .add("coordinates", coordinates(human.getCoordinates()))
                .add("creationDate", human.getCreationDate().toString())
                .add("realHero", human.isRealHero()).add("hasToothpick", human.isHasToothpick())
                .add("car", car(human.getCar())).add("minutesOfWaiting", human.getMinutesOfWaiting());
        if (human.getMood() == null) {
            json.addNull("mood");
        } else {
            json.add("mood", human.getMood().name());
        }
        if (human.getWeaponType() == null) {
            json.addNull("weaponType");
        } else {
            json.add("weaponType", human.getWeaponType().name());
        }
        if (human.getImpactSpeed() == null) {
            json.addNull("impactSpeed");
        } else {
            json.add("impactSpeed", human.getImpactSpeed().toString());
        }
        return json.build();
    }
}
