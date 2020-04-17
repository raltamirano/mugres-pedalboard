package mugres.pedalboard.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import mugres.core.common.*;
import mugres.core.function.builtin.drums.BlastBeat;
import mugres.core.function.builtin.drums.HalfTime;
import mugres.core.function.builtin.drums.PreRecordedDrums;
import mugres.core.live.processors.Processor;
import mugres.core.live.processors.drummer.Drummer;
import mugres.core.live.processors.drummer.commands.*;
import mugres.core.live.processors.drummer.config.Configuration;
import mugres.pedalboard.EntryPoint;
import mugres.pedalboard.config.DrummerConfig;
import mugres.pedalboard.config.MUGRESConfig;
import mugres.pedalboard.config.PedalboardConfig;
import mugres.pedalboard.controls.DrummerEditor;
import mugres.pedalboard.controls.DrummerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.System.currentTimeMillis;


public class PedalboardController
    implements DrummerEditor.Listener {
    @FXML
    private BorderPane root;

    @FXML
    private Button mainButton1;

    @FXML
    private Button mainButton2;

    @FXML
    private Button mainButton3;

    @FXML
    private Button mainButton4;

    @FXML
    private Button mainButton5;

    @FXML
    private ComboBox configurationsCombo;

    @FXML
    private Button editConfigurationButton;

    @FXML
    private Button deleteConfigurationButton;

    @FXML
    private HBox configurationControls;

    private Processor processor;

    private final Map<Integer, Pitch> buttonPitches = new HashMap<>();

    private int midiChannel = 1;

    private int velocity = 100;

    @FXML
    public void initialize() {
        HBox.setHgrow(mainButton1, Priority.ALWAYS);
        HBox.setHgrow(mainButton2, Priority.ALWAYS);
        HBox.setHgrow(mainButton3, Priority.ALWAYS);
        HBox.setHgrow(mainButton4, Priority.ALWAYS);
        HBox.setHgrow(mainButton5, Priority.ALWAYS);

        AnchorPane.setRightAnchor(configurationControls, 1.0);

        configurationsCombo.setConverter(new StringConverter<PedalboardConfig>() {
            @Override
            public String toString(final PedalboardConfig pedalboardConfig) {
                return pedalboardConfig != null ? pedalboardConfig.getName() : "";
            }
            @Override
            public PedalboardConfig fromString(final String s) {
                return null;
            }
        });

        loadConfigurations(null);
    }

    private void loadConfigurations(final String selectedConfiguration) {
        final List<PedalboardConfig> pedalboards = EntryPoint.getMUGRESApplication()
                .getMUGRESConfig().getPedalboardConfigs();

        editConfigurationButton.setDisable(true);
        deleteConfigurationButton.setDisable(true);
        clearButtonsTooltips();
        processor = null;

        configurationsCombo.getItems().clear();
        configurationsCombo.getItems().addAll(pedalboards);

        if (pedalboards.size() == 1) {
            final PedalboardConfig pedalboardConfig = pedalboards.get(0);
            configurationsCombo.setValue(pedalboardConfig);
            loadConfiguration(pedalboardConfig);
        } else if (selectedConfiguration != null) {
            final PedalboardConfig pedalboardConfig = pedalboards.stream()
                    .filter(c -> c.getName().equals(selectedConfiguration))
                    .findFirst()
                    .orElse(null);
            configurationsCombo.setValue(pedalboardConfig);
            if (pedalboardConfig != null)
                loadConfiguration(pedalboardConfig);
        }
    }

    private void loadConfiguration(final PedalboardConfig pedalboardConfig) {
        editConfigurationButton.setDisable(false);
        deleteConfigurationButton.setDisable(false);

        processor = null;
        root.setCenter(null);
        clearButtonsTooltips();

        if (pedalboardConfig.getProcessor() == PedalboardConfig.Processor.DRUMMER) {
            setDrummerButtonPitches();

            final Configuration config = new Configuration(pedalboardConfig.getName());

            final Context context = Context.createBasicContext();
            for(final DrummerConfig.Control control : pedalboardConfig.getDrummerConfig().getControls()) {
                setButtonLabel(control);

                switch(control.getCommand()) {
                    case PLAY:
                        final PreRecordedDrums generator;
                        switch(control.getGenerator()){
                            case HALF_TIME:
                                generator = new HalfTime();
                                break;
                            case BLAST_BEAT:
                                generator = new BlastBeat();
                                break;
                            default:
                                throw new RuntimeException("Unknown generator function: " + control.getGenerator());

                        }

                        final Context playContext = Context.ComposableContext.of(context);
                        playContext.setTempo(control.getTempo());
                        playContext.setTimeSignature(control.getTimeSignature());

                        final String grooveName = control.getTitle();
                        config.createGroove(grooveName, playContext,
                                control.getLengthInMeasures(), generator);

                        config.setAction(buttonPitches.get(control.getNumber()).getMidi(),
                                Play.INSTANCE.action(
                                "pattern", grooveName,
                                "switchMode", control.getSwitchMode()));
                        break;

                    case HIT:
                        config.setAction(buttonPitches.get(control.getNumber()).getMidi(),
                                Hit.INSTANCE.action(
                                "options", control.getHitOptions(),
                                "velocity", control.getHitVelocity()));
                        break;
                    case FINISH:
                        config.setAction(buttonPitches.get(control.getNumber()).getMidi(),
                                Finish.INSTANCE.action());
                        break;
                    case STOP:
                        config.setAction(buttonPitches.get(control.getNumber()).getMidi(),
                                Stop.INSTANCE.action());
                        break;
                    case NOOP:
                        config.setAction(buttonPitches.get(control.getNumber()).getMidi(),
                                NoOp.INSTANCE.action());
                }
            }

            final Drummer drummer = new Drummer(context,
                    EntryPoint.getMUGRESApplication().getInput(),
                    EntryPoint.getMUGRESApplication().getOutput(),
                    config);

            processor = drummer;

            final DrummerPlayer drummerPlayer = new DrummerPlayer();
            drummerPlayer.setDrummer(drummer);
            root.setCenter(drummerPlayer);
        } else if (pedalboardConfig.getProcessor() == PedalboardConfig.Processor.TRANSFORMER) {
            throw new RuntimeException("Not implemented!");
        } else {
            throw new RuntimeException("Not implemented!");
        }
    }

    private void clearButtonsTooltips() {
        getMainButton(1).setTooltip(null);
        getMainButton(2).setTooltip(null);
        getMainButton(3).setTooltip(null);
        getMainButton(4).setTooltip(null);
        getMainButton(5).setTooltip(null);
    }

    private void setButtonLabel(final DrummerConfig.Control controlConfig) {
        String label = "";
        switch(controlConfig.getCommand()) {
            case PLAY:
                label = controlConfig.getTitle();
                break;
            case HIT:
                label = "Hit " + controlConfig.getHitOptions().stream().map(DrumKit::getName).
                        collect(Collectors.joining(" or "));
                break;
            case FINISH:
                label = "Finish";
                break;
            case STOP:
                label = "Stop now!";
                break;
            case NOOP:
                label = "Does nothing";
                break;
        }

        if (controlConfig.getTitle() != null && !controlConfig.getTitle().trim().isEmpty())
            label = controlConfig.getTitle();

        final Button button = getMainButton(controlConfig.getNumber());
        if (button != null)
            button.setTooltip(new Tooltip(label));
    }

    private Button getMainButton(final int number) {
        switch(number) {
            case 1: return mainButton1;
            case 2: return mainButton2;
            case 3: return mainButton3;
            case 4: return mainButton4;
            case 5: return mainButton5;
        }

        return null;
    }

    private void setDrummerButtonPitches() {
        buttonPitches.clear();
        buttonPitches.put(1, Pitch.of(60));
        buttonPitches.put(2, Pitch.of(61));
        buttonPitches.put(3, Pitch.of(62));
        buttonPitches.put(4, Pitch.of(63));
        buttonPitches.put(5, Pitch.of(64));
    }

    @FXML
    protected void onConfigurationSelected(final ActionEvent event) {
        final PedalboardConfig pedalboardConfiguration =
                (PedalboardConfig)configurationsCombo.getValue();

        if (pedalboardConfiguration != null)
            loadConfiguration(pedalboardConfiguration);
    }

    @FXML
    protected void onNewConfiguration(final ActionEvent event) {
        final DrummerEditor editor = new DrummerEditor();
        editor.addListener(this);
        root.setCenter(editor);
        configurationControls.setVisible(false);
    }

    @FXML
    protected void onEditConfiguration(final ActionEvent event) {
        final PedalboardConfig pedalboardConfiguration =
                (PedalboardConfig)configurationsCombo.getValue();

        final DrummerEditor editor = new DrummerEditor();
        editor.addListener(this);
        editor.setModel(pedalboardConfiguration);
        root.setCenter(editor);
        configurationControls.setVisible(false);
    }

    @FXML
    protected void onDeleteConfiguration(final ActionEvent event) {
        final PedalboardConfig pedalboardConfiguration =
                (PedalboardConfig)configurationsCombo.getValue();

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete configuration '" + pedalboardConfiguration.getName() + "'?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm deletion");
        setDefaultButton(alert, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.NO)
            return;

        final MUGRESConfig config = EntryPoint.getMUGRESApplication().getMUGRESConfig();
        config.getPedalboardConfigs().removeIf(c -> c.getName().equals(pedalboardConfiguration.getName()));
        config.save();

        loadConfigurations(null);
    }

    private static Alert setDefaultButton(final Alert alert, final ButtonType defaultButton) {
        DialogPane pane = alert.getDialogPane();
        for (final ButtonType t : alert.getButtonTypes())
            ((Button)pane.lookupButton(t)).setDefaultButton( t == defaultButton );
        return alert;
    }

    @FXML
    protected void onMainButton(final ActionEvent event) {
        if (processor == null)
            return;

        final Button button = (Button) event.getSource();
        final int buttonNumber = Integer.valueOf(button.getId()
                .replaceAll("[^\\d.]", ""));

        final Played played = Played.of(buttonPitches.get(buttonNumber), velocity);
        final Signal on = Signal.on(currentTimeMillis(), midiChannel, played);
        final Signal off = Signal.off(currentTimeMillis() + 500, midiChannel, played);

        processor.process(on);
        processor.process(off);
    }

    @Override
    public void onDrummerEditorCreate(final DrummerEditor editor) {
        root.setCenter(null);
        configurationControls.setVisible(true);

        final MUGRESConfig config = EntryPoint.getMUGRESApplication().getMUGRESConfig();
        config.getPedalboardConfigs().add(editor.getOutput());
        config.save();

        loadConfigurations(editor.getOutput().getName());
        configurationControls.setVisible(true);
    }

    @Override
    public void onDrummerEditorUpdate(final DrummerEditor editor) {
        root.setCenter(null);
        configurationControls.setVisible(true);

        final MUGRESConfig config = EntryPoint.getMUGRESApplication().getMUGRESConfig();
        config.getPedalboardConfigs().removeIf(c -> c.getName().equals(editor.getModel().getName()));
        config.getPedalboardConfigs().add(editor.getOutput());
        config.save();

        loadConfigurations(editor.getOutput().getName());
    }

    @Override
    public void onDrummerEditorCancel(final DrummerEditor editor) {
        root.setCenter(null);
        configurationControls.setVisible(true);

        if (editor.isEditing())
            loadConfiguration((PedalboardConfig) configurationsCombo.getValue());
    }
}