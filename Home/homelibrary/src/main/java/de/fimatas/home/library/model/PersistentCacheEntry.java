package de.fimatas.home.library.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class PersistentCacheEntry<T> {

    private LocalDateTime timestamp;

    private T value;

    public PersistentCacheEntry(LocalDateTime timestamp, T value) {
        this.timestamp = timestamp;
        this.value = value;
    }
}
