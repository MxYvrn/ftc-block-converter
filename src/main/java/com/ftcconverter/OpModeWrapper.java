package com.ftcconverter;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Wraps generated Java body in a valid LinearOpMode class with hardware lookups. */
public class OpModeWrapper {

    private static final Pattern MOTOR_HINTS = Pattern.compile(
        "\\b([A-Za-z_][A-Za-z_0-9]*)\\.(setMode|setTargetPosition|getCurrentPosition|isBusy"
      + "|setZeroPowerBehavior|setDirection)\\b");
    private static final Pattern MOTOR_SETPOWER =
        Pattern.compile("\\b([A-Za-z_][A-Za-z_0-9]*)\\.setPower\\b");
    private static final Pattern SERVO =
        Pattern.compile("\\b([A-Za-z_][A-Za-z_0-9]*)\\.setPosition\\b");
    private static final Pattern IMU =
        Pattern.compile("\\b([A-Za-z_][A-Za-z_0-9]*)\\.(resetYaw|getRobotYawPitchRollAngles)\\b");
    private static final Pattern DIST =
        Pattern.compile("\\b([A-Za-z_][A-Za-z_0-9]*)\\.getDistance\\b");
    private static final Pattern TOUCH =
        Pattern.compile("\\b([A-Za-z_][A-Za-z_0-9]*)\\.isPressed\\b");
    private static final Pattern COLOR =
        Pattern.compile("\\b([A-Za-z_][A-Za-z_0-9]*)\\.(red|green|blue|alpha)\\(\\)");
    private static final Pattern TIMER =
        Pattern.compile("\\b([A-Za-z_][A-Za-z_0-9]*)\\s*=\\s*new\\s+ElapsedTime");

    public static String wrap(String className, String body) {
        Set<String> motors = collect(MOTOR_HINTS, body);
        motors.addAll(collect(MOTOR_SETPOWER, body));
        Set<String> servos  = collect(SERVO, body); servos.removeAll(motors);
        Set<String> imus    = collect(IMU, body);
        Set<String> dists   = collect(DIST, body);
        Set<String> touches = collect(TOUCH, body);
        Set<String> colors  = collect(COLOR, body);
        Set<String> timers  = collect(TIMER, body);

        StringBuilder sb = new StringBuilder();
        sb.append("package org.firstinspires.ftc.teamcode;\n\n");
        sb.append("import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;\n");
        sb.append("import com.qualcomm.robotcore.eventloop.opmode.TeleOp;\n");
        sb.append("import com.qualcomm.robotcore.hardware.DcMotor;\n");
        sb.append("import com.qualcomm.robotcore.hardware.DcMotorSimple;\n");
        sb.append("import com.qualcomm.robotcore.hardware.Servo;\n");
        if (!imus.isEmpty()) sb.append("import com.qualcomm.robotcore.hardware.IMU;\n");
        if (!dists.isEmpty()) sb.append("import com.qualcomm.robotcore.hardware.DistanceSensor;\n");
        if (!touches.isEmpty()) sb.append("import com.qualcomm.robotcore.hardware.TouchSensor;\n");
        if (!colors.isEmpty()) sb.append("import com.qualcomm.robotcore.hardware.ColorSensor;\n");
        if (!timers.isEmpty()) sb.append("import com.qualcomm.robotcore.util.ElapsedTime;\n");
        if (!imus.isEmpty()) sb.append("import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;\n");
        if (!dists.isEmpty()) sb.append("import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;\n");
        sb.append("\n@TeleOp(name=\"").append(className).append("\")\n");
        sb.append("public class ").append(className).append(" extends LinearOpMode {\n");
        sb.append("    @Override public void runOpMode() {\n");
        declare(sb, "DcMotor", motors);
        declare(sb, "Servo", servos);
        declare(sb, "IMU", imus);
        declare(sb, "DistanceSensor", dists);
        declare(sb, "TouchSensor", touches);
        declare(sb, "ColorSensor", colors);
        sb.append('\n');
        for (String line : body.split("\n")) sb.append("        ").append(line).append('\n');
        sb.append("    }\n}\n");
        return sb.toString();
    }

    private static void declare(StringBuilder sb, String type, Set<String> names) {
        for (String n : names)
            sb.append("        ").append(type).append(' ').append(n)
              .append(" = hardwareMap.get(").append(type).append(".class, \"")
              .append(n).append("\");\n");
    }

    private static Set<String> collect(Pattern p, String body) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = p.matcher(body);
        while (m.find()) out.add(m.group(1));
        return out;
    }
}
