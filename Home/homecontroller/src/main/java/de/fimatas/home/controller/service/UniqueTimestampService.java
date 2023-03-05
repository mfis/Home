package de.fimatas.home.controller.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public class UniqueTimestampService {

    private LocalDateTime last = LocalDateTime.now();

    public LocalDateTime get(){
        var unique = LocalDateTime.now();
        var millisBetween = ChronoUnit.MILLIS.between(last, unique);
        if(millisBetween < 1) {
            unique = last.plusNanos(1000000);
        }
        last = unique;
        return unique;
    }

    public String getAsStringWithMillis(){
        return get().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    public static String getAsStringWithMillis(LocalDateTime ldt){
        return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }
}
