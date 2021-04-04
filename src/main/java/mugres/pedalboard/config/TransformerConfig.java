package mugres.pedalboard.config;

import mugres.core.common.Note;
import mugres.core.common.Pitch;

import java.util.*;

public class TransformerConfig {
    private List<Button> buttons = new ArrayList<>();
    private List<Filter> filters = new ArrayList<>();

    public List<Button> getButtons() {
        return buttons;
    }

    public void setButtons(List<Button> buttons) {
        this.buttons = buttons;
    }

    public Button getButton(final int number) {
        return buttons.stream().filter(b -> b.number == number).findFirst().orElse(null);
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public static class Button {
        private int number;
        private int midi;
        private String label;

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public int getMidi() {
            return midi;
        }

        public void setMidi(int midi) {
            this.midi = midi;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Pitch getPitch() {
            return Pitch.of(midi);
        }
    }

    public static class Filter {
        private String filter;
        private Map<String, Object> arguments = new HashMap<>();

        public String getFilter() {
            return filter;
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }
    }
}
