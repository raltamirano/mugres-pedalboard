package com.raltamirano.midipedalboard.model;

import lombok.Data;
import lombok.NonNull;

import javax.sound.midi.Sequence;
import java.util.HashMap;
import java.util.Map;

import static com.raltamirano.midipedalboard.model.Pattern.Mode.SEQUENCE;

@Data
public class Song {
    @NonNull
    private String title;
    @NonNull
    private int tempo;
    @NonNull
    private Map<String, Pattern> patterns = new HashMap<>();

    @NonNull
    public Pattern createPattern(@NonNull final String name) {
        return createPattern(name, 0, SEQUENCE, SEQUENCE);
    }

    @NonNull
    public Pattern createPattern(@NonNull final String name,
                                 @NonNull final Pattern.Mode groovesMode,
                                 @NonNull final Pattern.Mode fillsMode) {
        return createPattern(name, 0, groovesMode, fillsMode);
    }

    @NonNull
    public Pattern createPattern(@NonNull final String name, final int tempo,
                                 @NonNull final Pattern.Mode groovesMode,
                                 @NonNull final Pattern.Mode fillsMode) {
        if (patterns.containsKey(name))
            throw new IllegalArgumentException("Pattern already created: " + name);

        final Pattern pattern = new Pattern(name, tempo, groovesMode, fillsMode);
        patterns.put(name, pattern);

        return pattern;
    }

    @NonNull public Pattern getPattern(@NonNull final String name) {
        final Pattern pattern = patterns.get(name);

        if (pattern == null)
            throw new IllegalArgumentException("Unknown pattern: " + name);

        return pattern;
    }
}
