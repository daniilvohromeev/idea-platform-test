package org.example;

import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import org.json.*;

public class FlightStatusChecker {
    private static final int MAX_WIND_SPEED = 30;
    private static final int MIN_VISIBILITY = 200;

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        ConcurrentHashMap<String, String> flightStatuses = new ConcurrentHashMap<>();

        // Чтение и парсинг файла
        Future<String> fileContentFuture = executorService.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new FileReader("flights_and_forecast.json"))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                return content.toString();
            }
        });

        String fileContent = fileContentFuture.get();
        JSONObject jsonObject = new JSONObject(fileContent);
        JSONArray flightsArray = jsonObject.getJSONArray("flights");
        JSONObject forecastObject = jsonObject.getJSONObject("forecast");

        // Обработка рейсов
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < flightsArray.length(); i++) {
            JSONObject flight = flightsArray.getJSONObject(i);
            futures.add(executorService.submit(() -> {
                String flightNo = flight.getString("no");
                String from = flight.getString("from");
                String to = flight.getString("to");
                int departure = flight.getInt("departure");
                int duration = flight.getInt("duration");

                boolean departureWeather = checkWeather(forecastObject, from, departure);
                boolean arrivalWeather = checkWeather(forecastObject, to, departure + duration);

                flightStatuses.put(flightNo, flightNo + " | " + from + " -> " + to + " | " +
                        (departureWeather && arrivalWeather ? "по расписанию" : "отменен"));
                return null;
            }));
        }

        // Шаг 3: Ожидание завершения всех задач
        for (Future<Void> future : futures) {
            future.get();
        }

        // Шаг 4: Запись результатов в файл
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("flights_status.txt"))) {
            flightStatuses.forEach((k, v) -> {
                try {
                    writer.write(v);
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        executorService.shutdown();
    }

    private static boolean checkWeather(JSONObject forecastObject, String city, int time) {
        JSONArray cityForecast = forecastObject.getJSONArray(city);
        for (int i = 0; i < cityForecast.length(); i++) {
            JSONObject weather = cityForecast.getJSONObject(i);
            if (weather.getInt("time") == time) {
                int wind = weather.getInt("wind");
                int visibility = weather.getInt("visibility");
                return wind <= MAX_WIND_SPEED && visibility >= MIN_VISIBILITY;
            }
        }
        return false;
    }
}
