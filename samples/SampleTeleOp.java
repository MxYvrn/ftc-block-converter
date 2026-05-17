package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp(name="SampleTeleOp")
public class SampleTeleOp extends LinearOpMode {
    @Override public void runOpMode() {
        DcMotor leftDrive  = hardwareMap.get(DcMotor.class, "leftDrive");
        DcMotor rightDrive = hardwareMap.get(DcMotor.class, "rightDrive");
        Servo claw         = hardwareMap.get(Servo.class, "claw");

        waitForStart();
        while (opModeIsActive()) {
            double drive = -gamepad1.left_stick_y;
            double turn  = gamepad1.right_stick_x;
            leftDrive.setPower(drive + turn);
            rightDrive.setPower(drive - turn);

            if (gamepad1.a) {
                claw.setPosition(0.8);
            } else if (gamepad1.b) {
                claw.setPosition(0.2);
            } else {
                claw.setPosition(0.5);
            }

            telemetry.addData("drive", drive);
            telemetry.addData("turn", turn);
            telemetry.update();
        }
    }
}
