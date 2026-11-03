package com.pap.web.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/** Template sample endpoint kept for route parity with the .NET API; not used by the UI. */
@Hidden
@RestController
@RequestMapping("/WeatherForecast")
public class WeatherForecastController {

    private static final List<String> SUMMARIES = List.of(
            "Freezing", "Bracing", "Chilly", "Cool", "Mild", "Warm", "Balmy", "Hot", "Sweltering", "Scorching");

    public record WeatherForecast(LocalDate date, int temperatureC, int temperatureF, String summary) {
    }

    @GetMapping
    public List<WeatherForecast> get() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return IntStream.rangeClosed(1, 5)
                .mapToObj(day -> {
                    int celsius = random.nextInt(-20, 55);
                    return new WeatherForecast(LocalDate.now().plusDays(day), celsius,
                            32 + (int) (celsius / 0.5556), SUMMARIES.get(random.nextInt(SUMMARIES.size())));
                })
                .toList();
    }
}
