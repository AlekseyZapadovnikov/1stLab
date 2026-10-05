package ru.itmo.web;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.itmo.entity.Car;
import ru.itmo.entity.Coordinates;
import ru.itmo.entity.HumanBeing;
import ru.itmo.entity.Mood;
import ru.itmo.entity.WeaponType;
import ru.itmo.service.HumanBeingServiceLocal;

import java.io.StringReader;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

@Path("/")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HumanBeingResource {
    @EJB
    private HumanBeingServiceLocal service;

    @GET
    @Path("humans")
    public JsonObject list(@QueryParam("page") @DefaultValue("1") int page,
                           @QueryParam("size") @DefaultValue("10") int size,
                           @QueryParam("name") @DefaultValue("") String name,
                           @QueryParam("mood") @DefaultValue("") String mood,
                           @QueryParam("weaponType") @DefaultValue("") String weaponType,
                           @QueryParam("sort") @DefaultValue("id") String sort,
                           @QueryParam("direction") @DefaultValue("asc") String direction) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BadRequestException("Номер страницы должен быть положительным, размер — от 1 до 100");
        }
        Comparator<HumanBeing> comparator = comparator(sort);
        if ("desc".equals(direction)) {
            comparator = comparator.reversed();
        } else if (!"asc".equals(direction)) {
            throw new BadRequestException("Направление сортировки: asc или desc");
        }
        List<HumanBeing> humans = service.findAllHumanBeings().stream()
                .filter(human -> contains(human.getName(), name))
                .filter(human -> contains(enumText(human.getMood()), mood))
                .filter(human -> contains(enumText(human.getWeaponType()), weaponType))
                .sorted(comparator.thenComparingInt(HumanBeing::getId)).toList();
        int pages = Math.max(1, (int) ((humans.size() + (long) size - 1) / size));
        int actualPage = Math.min(page, pages);
        int start = (actualPage - 1) * size;
        JsonArrayBuilder items = Json.createArrayBuilder();
        humans.subList(start, Math.min(start + size, humans.size()))
                .forEach(human -> items.add(EntityJson.human(human)));
        return Json.createObjectBuilder().add("items", items).add("total", humans.size())
                .add("page", actualPage).add("pages", pages).add("size", size).build();
    }

    @GET
    @Path("humans/{id}")
    public JsonObject find(@PathParam("id") int id) {
        return EntityJson.human(requireHuman(id));
    }

    @POST
    @Path("humans")
    public Response create(String input) {
        HumanBeing saved = service.saveHumanBeing(readHuman(input));
        ChangesEndpoint.publish();
        return Response.status(Response.Status.CREATED).entity(EntityJson.human(saved)).build();
    }

    @PUT
    @Path("humans/{id}")
    public JsonObject update(@PathParam("id") int id, String input) {
        positiveId(id);
        HumanBeing human = readHuman(input);
        human.setId(id);
        HumanBeing saved = service.updateHumanBeing(human);
        ChangesEndpoint.publish();
        return EntityJson.human(saved);
    }

    @DELETE
    @Path("humans/{id}")
    public Response delete(@PathParam("id") int id) {
        positiveId(id);
        if (!service.deleteHumanBeing(id)) {
            throw new NotFoundException("Человек с ID " + id + " не найден");
        }
        ChangesEndpoint.publish();
        return Response.noContent().build();
    }

    @GET
    @Path("relations")
    public JsonObject relations() {
        JsonArrayBuilder cars = Json.createArrayBuilder();
        service.findAllCars().forEach(car -> cars.add(EntityJson.car(car)));
        JsonArrayBuilder coordinates = Json.createArrayBuilder();
        service.findAllCoordinates().forEach(value -> coordinates.add(EntityJson.coordinates(value)));
        JsonArrayBuilder moods = Json.createArrayBuilder();
        for (Mood mood : Mood.values()) {
            moods.add(mood.name());
        }
        JsonArrayBuilder weapons = Json.createArrayBuilder();
        for (WeaponType weapon : WeaponType.values()) {
            weapons.add(weapon.name());
        }
        return Json.createObjectBuilder().add("cars", cars).add("coordinates", coordinates)
                .add("moods", moods).add("weaponTypes", weapons).build();
    }

    @GET
    @Path("special/average")
    public JsonObject average() {
        OptionalDouble average = service.averageMinutesOfWaiting();
        JsonObjectBuilder json = Json.createObjectBuilder();
        if (average.isPresent()) {
            json.add("average", average.getAsDouble());
        } else {
            json.addNull("average");
        }
        return json.build();
    }

    @GET
    @Path("special/groups")
    public JsonObject groups() {
        JsonArrayBuilder groups = Json.createArrayBuilder();
        service.countHumanBeingsByName().forEach((name, count) -> groups.add(
                Json.createObjectBuilder().add("name", name).add("count", count)));
        return Json.createObjectBuilder().add("groups", groups).build();
    }

    @GET
    @Path("special/search")
    public JsonObject search(@QueryParam("substring") String substring) {
        if (substring == null) {
            throw new BadRequestException("Укажите подстроку имени");
        }
        JsonArrayBuilder items = Json.createArrayBuilder();
        for (HumanBeing human : service.findHumanBeingsByNameSubstring(substring)) {
            items.add(EntityJson.human(human));
        }
        return Json.createObjectBuilder().add("items", items).build();
    }

    @POST
    @Path("special/delete-without-toothpicks")
    public JsonObject deleteWithoutToothpicks() {
        int count = service.deleteHeroesWithoutToothpicks();
        if (count > 0) {
            ChangesEndpoint.publish();
        }
        return Json.createObjectBuilder().add("count", count).build();
    }

    @POST
    @Path("special/make-gloomy")
    public JsonObject makeGloomy() {
        int count = service.makeHeroesGloomy();
        if (count > 0) {
            ChangesEndpoint.publish();
        }
        return Json.createObjectBuilder().add("count", count).build();
    }

    private HumanBeing readHuman(String body) {
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Требуется корректный JSON-объект с данными человека");
        }
        JsonObject input;
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonValue value = reader.readValue();
            if (!(value instanceof JsonObject object)) {
                throw new BadRequestException("Требуется корректный JSON-объект с данными человека");
            }
            input = object;
        } catch (JsonException exception) {
            throw new BadRequestException("Требуется корректный JSON-объект с данными человека");
        }
        JsonFields.reject(input, "id", "creationDate");
        HumanBeing human = new HumanBeing();
        human.setName(JsonFields.string(input, "name"));
        human.setRealHero(JsonFields.bool(input, "realHero"));
        human.setHasToothpick(JsonFields.bool(input, "hasToothpick"));
        human.setMood(JsonFields.optionalEnum(input, "mood", Mood.class));
        human.setWeaponType(JsonFields.optionalEnum(input, "weaponType", WeaponType.class));
        human.setImpactSpeed(JsonFields.optionalLong(input, "impactSpeed"));
        human.setMinutesOfWaiting(JsonFields.decimal(input, "minutesOfWaiting"));
        human.setCar(readCar(JsonFields.object(input, "car")));
        human.setCoordinates(readCoordinates(JsonFields.object(input, "coordinates")));
        return human;
    }

    private Car readCar(JsonObject input) {
        if (input.containsKey("id")) {
            if (input.containsKey("cool")) {
                throw new BadRequestException("Для существующей машины передайте только ID");
            }
            int id = JsonFields.positiveId(input, "id");
            return service.findCarById(id).orElseThrow(() ->
                    new NotFoundException("Машина с ID " + id + " не найдена"));
        }
        Car car = new Car();
        car.setCool(JsonFields.bool(input, "cool"));
        return car;
    }

    private Coordinates readCoordinates(JsonObject input) {
        if (input.containsKey("id")) {
            if (input.containsKey("x") || input.containsKey("y")) {
                throw new BadRequestException("Для существующих координат передайте только ID");
            }
            int id = JsonFields.positiveId(input, "id");
            return service.findCoordinatesById(id).orElseThrow(() ->
                    new NotFoundException("Координаты с ID " + id + " не найдены"));
        }
        Coordinates coordinates = new Coordinates();
        coordinates.setX(JsonFields.longValue(input, "x"));
        coordinates.setY(JsonFields.floatValue(input, "y"));
        return coordinates;
    }

    private HumanBeing requireHuman(int id) {
        positiveId(id);
        return service.findHumanBeingById(id).orElseThrow(() ->
                new NotFoundException("Человек с ID " + id + " не найден"));
    }

    private void positiveId(int id) {
        if (id <= 0) {
            throw new BadRequestException("ID должен быть положительным целым числом");
        }
    }

    private Comparator<HumanBeing> comparator(String sort) {
        return switch (sort) {
            case "id" -> Comparator.comparingInt(HumanBeing::getId);
            case "name" -> Comparator.comparing(HumanBeing::getName, String.CASE_INSENSITIVE_ORDER);
            case "mood" -> Comparator.comparing(human -> enumText(human.getMood()));
            case "weaponType" -> Comparator.comparing(human -> enumText(human.getWeaponType()));
            default -> throw new BadRequestException("Неизвестное поле сортировки");
        };
    }

    private String enumText(Enum<?> value) {
        if (value == null) {
            return "";
        }
        return value.name();
    }

    private boolean contains(String value, String filter) {
        return value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }
}
