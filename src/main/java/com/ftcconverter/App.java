package com.ftcconverter;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URL;

public class App extends Application {

    private WebEngine engine;
    private TextArea javaArea;
    private TextField classNameField;
    private Label status;

    @Override
    public void start(Stage stage) {
        WebView web = new WebView();
        engine = web.getEngine();
        URL idx = getClass().getResource("/blockly/index.html");
        engine.load(idx.toExternalForm());

        javaArea = new TextArea(sampleJava());
        javaArea.setStyle("-fx-font-family: 'Menlo', 'Consolas', monospace;");
        javaArea.setPrefColumnCount(50);

        classNameField = new TextField("MyTeleOp");
        Button toBlocks = new Button("Java → Blocks");
        Button toJava   = new Button("Blocks → Java");
        status = new Label("Loading Blockly…");

        engine.getLoadWorker().stateProperty().addListener((o, a, s) -> {
            if (s == Worker.State.SUCCEEDED) status.setText("Ready.");
            if (s == Worker.State.FAILED)    status.setText("Blockly failed to load.");
        });

        toBlocks.setOnAction(e -> {
            try {
                String xml = JavaToBlocks.convert(javaArea.getText());
                JSObject win = (JSObject) engine.executeScript("window");
                Object res = win.call("loadXml", xml);
                status.setText(String.valueOf(res).isEmpty() ? "Loaded into Blockly." : String.valueOf(res));
            } catch (Exception ex) {
                status.setText("Java→Blocks error: " + ex.getMessage());
            }
        });

        toJava.setOnAction(e -> {
            try {
                String body = (String) engine.executeScript("window.getJava()");
                String cls = classNameField.getText().isBlank() ? "MyTeleOp" : classNameField.getText();
                javaArea.setText(OpModeWrapper.wrap(cls, body));
                status.setText("Java generated.");
            } catch (Exception ex) {
                status.setText("Blocks→Java error: " + ex.getMessage());
            }
        });

        HBox controls = new HBox(8,
                new Label("Class:"), classNameField, toBlocks, toJava, status);
        controls.setPadding(new Insets(8));

        VBox left = new VBox(new Label("Java"), javaArea);
        VBox.setVgrow(javaArea, Priority.ALWAYS);
        left.setPadding(new Insets(8));
        left.setPrefWidth(520);

        SplitPane split = new SplitPane(left, web);
        split.setDividerPositions(0.42);

        BorderPane root = new BorderPane();
        root.setTop(controls);
        root.setCenter(split);

        stage.setScene(new Scene(root, 1300, 800));
        stage.setTitle("FTC Java ↔ Blocks Converter");
        stage.show();
    }

    private static String sampleJava() {
        return """
            package org.firstinspires.ftc.teamcode;
            import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
            import com.qualcomm.robotcore.hardware.DcMotor;

            public class MyTeleOp extends LinearOpMode {
                @Override public void runOpMode() {
                    DcMotor leftDrive = hardwareMap.get(DcMotor.class, "leftDrive");
                    waitForStart();
                    while (opModeIsActive()) {
                        double power = -gamepad1.left_stick_y;
                        leftDrive.setPower(power);
                        telemetry.addData("power", power);
                        telemetry.update();
                    }
                }
            }
            """;
    }

    public static void main(String[] args) { launch(args); }
}
