package org.example;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class FlightForecastChecker {

    private static final HashMap<String, ZoneId> cityTimeZones = new HashMap<>();
    static {
        cityTimeZones.put("moscow", ZoneId.of("Europe/Moscow"));
        cityTimeZones.put("novosibirsk", ZoneId.of("Asia/Novosibirsk"));
        cityTimeZones.put("omsk", ZoneId.of("Asia/Omsk"));
    }

    public static void main(String[] args) {
        String content;
        try {
            content = new String(Files.readAllBytes(Paths.get("flights_and_forecast.json")));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        JSONObject jsonObject = new JSONObject(content);
        JSONArray flights = jsonObject.getJSONArray("flights");
        JSONObject forecast = jsonObject.getJSONObject("forecast");

        ExecutorService executor = Executors.newFixedThreadPool(flights.length());
        List<Future<String>> results = new ArrayList<>();

        for (int i = 0; i < flights.length(); i++) {
            JSONObject flight = flights.getJSONObject(i);
            results.add(executor.submit(() -> checkFlightStatus(flight, forecast)));
        }

        results.forEach(future -> {
            try {
                System.out.println(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        executor.shutdown();
    }

    private static String checkFlightStatus(JSONObject flight, JSONObject forecast) {
        String no = flight.getString("no");
        int departureTime = flight.getInt("departure");
        String fromCity = flight.getString("from");
        String toCity = flight.getString("to");
        int duration = flight.getInt("duration");

        // Для проверки погоды в городе отправления
        boolean departureWeather = checkWeatherConditions(forecast.getJSONArray(fromCity), departureTime, cityTimeZones.get(fromCity), cityTimeZones.get(fromCity));

        // Для проверки погоды в городе прибытия
        boolean arrivalWeather = checkWeatherConditions(forecast.getJSONArray(toCity), departureTime + duration, cityTimeZones.get(fromCity), cityTimeZones.get(toCity));

        String status = (departureWeather && arrivalWeather) ? "по расписанию" : "отменен";
        return String.format("%s | %s -> %s | %s", no, fromCity, toCity, status);
    }

    private static boolean checkWeatherConditions(JSONArray cityForecast, int time, ZoneId departureZoneId, ZoneId arrivalZoneId) {
        // Сначала создаём метку времени отправления в часовом поясе города отправления
        ZonedDateTime departureDateTime = ZonedDateTime.now(departureZoneId).withHour(time % 24);

        // Затем преобразуем это время в часовой пояс города прибытия
        ZonedDateTime arrivalDateTime = departureDateTime.withZoneSameInstant(arrivalZoneId);

        // Используем час прибытия для проверки погоды
        for (int i = 0; i < cityForecast.length(); i++) {
            JSONObject weather = cityForecast.getJSONObject(i);
            int forecastTime = weather.getInt("time");
            if (forecastTime == arrivalDateTime.getHour()) {
                int wind = weather.getInt("wind");
                int visibility = weather.getInt("visibility");
                return wind <= 30 && visibility >= 200;
            }
        }
        return false;
    }
}
