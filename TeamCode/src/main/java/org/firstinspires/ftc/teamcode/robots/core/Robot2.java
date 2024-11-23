package org.firstinspires.ftc.teamcode.robots.core;

import static org.firstinspires.ftc.teamcode.util.utilMethods.futureTime;
import static org.firstinspires.ftc.teamcode.util.utilMethods.isPast;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.robots.csbot.Field;
import org.firstinspires.ftc.teamcode.robots.deepthought.subsystem.Robot;

import java.util.LinkedHashMap;
import java.util.Map;


public class Robot2 extends OpMode {
    HardwareMap hardwareMap;
    DcMotorEx leftFront, leftBack, rightFront, rightBack;
    Servo claw;
    Gamepad gamepad1;
    DcMotorEx shoulder;
    DcMotorEx elbow;
    DcMotorEx slide;
    public double clawOpenPosition = 1;
    public double clawClosePosition = 0.4;
    private Robot robot;

    public static int autonIndex;
    long autonTimer = futureTime(10);
    public static double adjust_time = 1.0;
    public static double powerOne = 0.1;
    public static int timeOne = 1;
    public static int timeTurn = 1;
    public static int slideUp = 200;
    public static int shoulderUp = 100;
    public static int elbowUp = 200;
    public static int slideRe = 0;
    public static int shoulderDown = 75;
    public static int elbowDown = 50;


    public Robot2(HardwareMap hardwareMap) {
        this.hardwareMap = hardwareMap;
        autonIndex = 0;
    }

    public void execute(){
        switch(autonIndex){
            case 0:
                // Move strafe to the left side
                leftFront.setPower(powerOne); //RIGHTFRONT
                rightFront.setPower(-powerOne); //LEFT FRONT
                leftBack.setPower(-powerOne); //RIGHTBACK
                rightBack.setPower(powerOne); //

                //if (rightBack.getCurrentPosition() == rightBack.getTargetPosition()){
                    //autonIndex ++;
                //}
                // NUMBER OF TICKS (AMOUNT) IT MOVES PER FOOT

                break;

            case 1:
                // Turn -135 degrees
                leftFront.setPower(powerOne);
                rightFront.setPower(powerOne);
                leftBack.setPower(powerOne);
                rightBack.setPower(powerOne);

                //if (leftFront.getCurrentPosition() == leftFront.getTargetPosition()){
                //    autonIndex ++;
                //}

                break;

            case 2:
                // Extend Linear Slide
                if (isPast(autonTimer)){
                    slide.setTargetPosition(slideUp);
                    shoulder.setTargetPosition(shoulderUp);
                    elbow.setTargetPosition(elbowUp);
                    if ((slide.getCurrentPosition() == slide.getTargetPosition())&(shoulder.getCurrentPosition()==shoulder.getTargetPosition())&(elbow.getCurrentPosition()==elbow.getTargetPosition())){
                        autonIndex++;
                    }
                }
                break;

            case 3:
                // Open claw to release block
                claw.setPosition(clawOpenPosition);
                autonIndex++;
                break;

            case 4:
                // Retract Linear Slide
                slide.setTargetPosition(slideRe);
                if ((slide.getCurrentPosition() == slide.getTargetPosition())){
                    autonIndex++;
                }
                break;

            /*case 5:
                // Turn 45 degrees
                if (isPast(autonTimer)){
                    leftFront.setPower(-powerOne);
                    rightFront.setPower(-powerOne);
                    leftBack.setPower(-powerOne);
                    rightBack.setPower(-powerOne);
                    autonTimer = futureTime((float) timeTurn /3);
                    autonIndex++;
                }
                break;

            case 6:
                // Move forward a unit
                if (isPast(autonTimer)){
                    leftFront.setPower(powerOne);
                    rightFront.setPower(-powerOne);
                    leftBack.setPower(powerOne);
                    rightBack.setPower(-powerOne);
                    autonTimer = futureTime(adjust_time);
                    autonIndex++;
                }
                break;

            case 7:
                // Turn 90 degrees
                if (isPast(autonTimer)){
                    leftFront.setPower(-powerOne);
                    rightFront.setPower(-powerOne);
                    leftBack.setPower(-powerOne);
                    rightBack.setPower(-powerOne);
                    autonTimer = futureTime((float) (timeTurn * 2) /3);
                    autonIndex++;
                }
                break;

            case 8:
                // Move a unit
                if (isPast(autonTimer)){
                    leftFront.setPower(powerOne);
                    rightFront.setPower(-powerOne);
                    leftBack.setPower(powerOne);
                    rightBack.setPower(-powerOne);
                    autonTimer = futureTime(timeOne);
                    autonIndex++;
                }
                break;

            case 9:
                // Extend Slide angled down
                if (isPast(autonTimer)) {
                    slide.setTargetPosition(slideUp);
                    shoulder.setTargetPosition(shoulderDown);
                    elbow.setTargetPosition(elbowDown);
                    if ((slide.getCurrentPosition() == slide.getTargetPosition()) & (shoulder.getCurrentPosition() == shoulder.getTargetPosition()) & (elbow.getCurrentPosition() == elbow.getTargetPosition())) {
                        autonIndex++;
                    }
                }
                break;

            case 10:
                // Close claw (hold brick)
                claw.setPosition(clawClosePosition);
                autonIndex++;
                break;

            case 11:
                // Retract slide
                slide.setTargetPosition(slideRe);
                if ((slide.getCurrentPosition() == slide.getTargetPosition())){
                    autonIndex++;
                }
                break;

            case 12:
                // Move back a unit
                leftFront.setPower(-powerOne);
                rightFront.setPower(powerOne);
                leftBack.setPower(-powerOne);
                rightBack.setPower(powerOne);
                autonTimer = futureTime(timeOne);
                autonIndex++;
                break;

            case 13:
                // Turn -180 degrees
                if (isPast(autonTimer)) {
                    leftFront.setPower(powerOne);
                    rightFront.setPower(powerOne);
                    leftBack.setPower(powerOne);
                    rightBack.setPower(powerOne);
                    autonTimer = futureTime((float) (timeOne * 4) /3);
                    autonIndex++;
                    //autonIndex=autonIndex-13;
                    //adjust_time = 0.75;
                }
                break;*/

            default: break;
        }
    }

    public void init() {
        leftFront = hardwareMap.get(DcMotorEx.class, "leftFront");
        leftBack = hardwareMap.get(DcMotorEx.class, "leftBack");
        rightFront = hardwareMap.get(DcMotorEx.class, "rightFront");
        rightBack = hardwareMap.get(DcMotorEx.class, "rightBack");
        shoulder = hardwareMap.get(DcMotorEx.class, "shoulder");
        elbow = hardwareMap.get(DcMotorEx.class, "elbow");
        claw = hardwareMap.get(Servo.class, "claw");
        slide = hardwareMap.get(DcMotorEx.class, "slide");
        // Set motor runmodes
        leftBack.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        rightBack.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        leftFront.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        rightFront.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        shoulder.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        elbow.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        slide.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);

        shoulder.setPower(1);
        shoulder.setVelocity(800);
        shoulder.setTargetPosition(0);
        shoulder.setMode(DcMotorEx.RunMode.RUN_TO_POSITION);

        elbow.setPower(1);
        elbow.setVelocity(800);
        elbow.setTargetPosition(0);
        elbow.setMode(DcMotorEx.RunMode.RUN_TO_POSITION);

        slide.setPower(1);
        slide.setVelocity(150);
        slide.setTargetPosition(0);
        slide.setMode(DcMotorEx.RunMode.RUN_TO_POSITION);

        leftFront.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        leftBack.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        rightFront.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        rightBack.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);

    }

    @Override
    public void loop() {

        execute();
           /* if (execute() == true) {
                shoulderTargetPosition = 50;
                elbowTargetPosition = 75;
                slideTargetPosition = 50;
                clawOpen = true;
                shoulder.setTargetPosition(shoulderTargetPosition);
                elbow.setTargetPosition(elbowTargetPosition);
                slide.setTargetPosition(slideTargetPosition);
            }
            i++;
        }*/
    }

    public Map<String, Object> getTelemetry(boolean b) {
        LinkedHashMap<String, Object> telemetry = new LinkedHashMap<>();

        telemetry.put("Right Back", rightBack.getCurrentPosition());

        return telemetry;
    }

    public String getTelemetryName() {
        return "Core Robot";
    }
}




