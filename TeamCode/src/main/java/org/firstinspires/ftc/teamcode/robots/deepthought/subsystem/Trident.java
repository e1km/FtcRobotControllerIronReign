package org.firstinspires.ftc.teamcode.robots.deepthought.subsystem;

import static org.firstinspires.ftc.teamcode.util.utilMethods.futureTime;
import static org.firstinspires.ftc.teamcode.util.utilMethods.isPast;
import static org.firstinspires.ftc.teamcode.util.utilMethods.withinError;
import android.graphics.Color;
import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.CRServoImplEx;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.robots.deepthought.util.Joint;
import org.firstinspires.ftc.teamcode.robots.r2v2.util.Utils;

import java.util.*;


@Config(value = "00_ITD_TRIDENT")
public class Trident implements Subsystem {
    HardwareMap hardwareMap;
    Robot robot;

    public DcMotorEx slide = null;
    public DcMotorEx crane = null;
    public Joint elbow;
    public Joint wrist;
    public CRServoImplEx beater = null;
    public Servo pincer;

    public NormalizedColorSensor colorSensor = null;
    public static boolean colorSensorEnabled = false;

    public static int colorSensorGain = 12;


    public enum CurrentSample {
        RED, BLUE, NEUTRAL, NO_SAMPLE
    }
    public CurrentSample currentSample = CurrentSample.NO_SAMPLE;
    public List<CurrentSample> targetSamples = new ArrayList<>();

    //SLIDE
    public static int slideTargetPosition = 0;
    public static int slidePositionMax = 1600;
    public static int slidePositionMin = 0;
    public static int slideSpeed = 15;
    public static double SLIDE_SPEED = 1500;


    //CRANE
    public static int craneTargetPosition = 0;
    public static int craneSpeed = 30;

    //ELBOW JOINT VARIABLES
    public static double ELBOW_START_ANGLE = 0;
    public static int ELBOW_HOME_POSITION = 2050;
    public static double ELBOW_PWM_PER_DEGREE = -5.672222222222222;
    public static double ELBOW_JOINT_SPEED = 60;
    public static double ELBOW_MIN_ANGLE = -15;
    public static double ELBOW_MAX_ANGLE = 220;
    public static int ELBOW_ADJUST_ANGLE = 5;
    public static double ELBOW_PREINTAKE_ANGLE = 123;


    //WRIST JOINT VARIABLES
    public static int WRIST_HOME_POSITION = 950;
    public static double WRIST_PWM_PER_DEGREE = 7.22222222222;
    public static double WRIST_START_ANGLE = 44;
    public static double WRIST_JOINT_SPEED = 50;
    public static double WRIST_MIN_ANGLE = 0;
    public static double WRIST_MAX_ANGLE = 140;
    public static int WRIST_ADJUST_ANGLE = 5;
    public static double WRIST_PREINTAKE_ANGLE = 123;


    //BEATER
    public static double beaterPower;

    //PINCER
    public static double PINCER_CLOSE = 1750;
    public static double PINCER_OPEN = 2150;
    public boolean pincerEnabled = true;


    public enum Articulation {
        MANUAL, //does nothing - used for transition tracking
        TUCK, //does nothing - used for transition tracking
        INTAKE,
        OUTTAKE
    }

    //LIVE STATES
    public Articulation articulation;
    private Articulation prevArticulation;

    public Articulation articulate(Articulation articulation) {
        this.articulation = articulation;
        return articulation;
    }

    public Articulation articulate() {
        switch (articulation) {
            case MANUAL:
                break;
            case TUCK:
                break;
            //SHOULD ONLY BE ACCESSED BY SAMPLE()
            case INTAKE:
                if (intake()) {
                    articulation = Articulation.MANUAL;
                }
                break;

            case OUTTAKE:
                if(outtake())
                    articulation = Articulation.MANUAL;
                break;
            default:
                throw new RuntimeException("how the ^%*( did you get here?");
        }
        prevArticulation = articulation;
        return articulation;
    }

    public static int intakeIndex;
    public long intakeTimer;
    public boolean intake() {
        switch (intakeIndex) {
            case 0:
                craneTargetPosition = 0;
                slideTargetPosition = 0;
                wrist.setTargetAngle(WRIST_PREINTAKE_ANGLE);
                elbow.setTargetAngle(ELBOW_PREINTAKE_ANGLE);

                //get to position
                if (withinError(crane.getCurrentPosition(), 0, 10)) {
                    intakeTimer = futureTime(5);
//                    intakeIndex++;
                }
                break;
            case 1:
                if (isPast(intakeTimer)) {
                    wrist.setTargetAngle(15);
                    elbow.setTargetAngle(180);
                }
                break;
            case 2:
//                beater.setPower(1);
//                colorSensorEnabled = true;
//                open the "gate"
//                if (targetSamples.contains(currentSample)) {
//                    intakeTimer = futureTime(3);
                intakeIndex++;
//                }
                break;
            case 3:
                //close the "gate"
//                beater.setPower(-1);
//                if (isPast(intakeTimer)) {
//                    beater.setPower(0);
                intakeIndex++;
//                }
                break;
            case 4:
                return true;
            case 5:
                break;
            case 6:
                return true;
        }
        return false;
    }

    public static int outtakeIndex;
    public long outtakeTimer;
    public boolean outtake() {
        switch (outtakeIndex) {
            case 0:
                craneTargetPosition = 0;
                slideTargetPosition = 0;
                wrist.setTargetAngle(WRIST_START_ANGLE);
                elbow.setTargetAngle(WRIST_START_ANGLE);

                //get to position
                if (withinError(crane.getCurrentPosition(), 0, 10)) {
                    outtakeIndex++;
                }
                break;
            case 1:
                craneTargetPosition = 630;
                if (withinError(crane.getCurrentPosition(), 630, 10)) {
                    outtakeTimer = futureTime(2);
                    outtakeIndex++;
                }
                break;
            case 2:
                wrist.setTargetAngle(10);
                elbow.setTargetAngle(110);
                if(isPast(outtakeTimer)){
                    outtakeIndex ++;
                }
                break;
            case 3:
                slide.setTargetPosition(-1230);
                if(withinError(slide.getCurrentPosition(), -1230, 10))
                    outtakeIndex++;
                break;
            case 4:
                return true;

        }
        return false;
    }

    public void sample(List<CurrentSample> samples) {
        articulation = Articulation.INTAKE;
        this.targetSamples = samples;
    }

    public void setTargetAngle(double elbowAngle, double wristAngle) {
        elbow.setTargetAngle(elbowAngle);
        wrist.setTargetAngle(wristAngle);
    }

    public Trident(HardwareMap hardwareMap, Robot robot) {
        this.hardwareMap = hardwareMap;
        this.robot = robot;
        elbow = new Joint(hardwareMap, "elbow", false, ELBOW_HOME_POSITION, ELBOW_PWM_PER_DEGREE, ELBOW_MIN_ANGLE, ELBOW_MAX_ANGLE, ELBOW_START_ANGLE, ELBOW_JOINT_SPEED);
        wrist = new Joint(hardwareMap, "wrist", false, WRIST_HOME_POSITION, WRIST_PWM_PER_DEGREE, WRIST_MIN_ANGLE, WRIST_MAX_ANGLE, WRIST_START_ANGLE, WRIST_JOINT_SPEED);
        slide = this.hardwareMap.get(DcMotorEx.class, "slide");
        crane = this.hardwareMap.get(DcMotorEx.class, "crane");
        colorSensor = this.hardwareMap.get(NormalizedColorSensor.class, "intakeSensor");
        beater = this.hardwareMap.get(CRServoImplEx.class, "beater");
        pincer = this.hardwareMap.get(Servo.class, "pincer");
        slide.setMotorEnable();
        slide.setPower(1);
        slide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        slide.setTargetPosition(0);
        slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        slide.setVelocity(SLIDE_SPEED);

        crane.setMotorEnable();
        crane.setPower(1);
        crane.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        crane.setTargetPosition(0);
        crane.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        crane.setVelocity(1500);
        articulation = Articulation.MANUAL;
    }

    public void adjustElbow(double angle) {
        elbow.setTargetAngle(elbow.getCurrentAngle() + angle);
    }

    public void adjustWrist(double angle) {
        wrist.setTargetAngle(wrist.getCurrentAngle() + angle);
    }

    @Override
    public void update(Canvas fieldOverlay) {
        //compute the current articulation/behavior
        articulate();
        //allow real-time flipper speed changes
        elbow.setSpeed(ELBOW_JOINT_SPEED);
        wrist.setSpeed(WRIST_JOINT_SPEED);

        if (colorSensorEnabled) {
            updateColorSensor();
            colorSensor.setGain(colorSensorGain);
        }


        //actually instruct actuators to go to desired targets
        elbow.update();
        wrist.update();
        slide.setTargetPosition(slideTargetPosition);
        crane.setTargetPosition(craneTargetPosition);
        beater.setPower(beaterPower);

//        if(pincerEnabled) {
//            pincer.setPosition(Utils.servoNormalize(PINCER_OPEN));
//        }
//        else {
//            pincer.setPosition(Utils.servoNormalize(PINCER_CLOSE));
//        }

    }

    public String updateColorSensor() {
        double hue = getHSV()[0];
        if(hue < 90 && hue > 70){
            currentSample = CurrentSample.NEUTRAL;
            return "NEUTRAL";
        }
        else if (hue < 60 && hue > 20) {
            currentSample = CurrentSample.RED;
            return "RED";
        }
        else if(hue < 250  && hue > 200) {
            currentSample = CurrentSample.BLUE;
            return "BLUE";
        }
        else {
            currentSample = CurrentSample.NO_SAMPLE ;
            return "NO SAMPLE";
        }
    }

    @Override
    public void stop() {
        slide.setMotorDisable();
    }

    public String HSVasString () {
        float[] hsv = getHSV();
        return hsv[0] + " " + hsv[1] + " " + hsv[2];
    }
    public float[] getHSV() {
        float[] hsv = new float[3];
        Color.colorToHSV(colorSensor.getNormalizedColors().toColor(), hsv);
        return hsv;
    }

    @Override
    public Map<String, Object> getTelemetry(boolean debug) {
        Map<String, Object> telemetryMap = new LinkedHashMap<>();
        telemetryMap.put("articulation", articulation.name());
        telemetryMap.put("intake index", intakeIndex);
        telemetryMap.put("outtake index", outtakeIndex);
        telemetryMap.put("crane target : real", "" + craneTargetPosition + " : " + crane.getCurrentPosition());
        telemetryMap.put("slide target : real", slideTargetPosition + " : " + slide.getCurrentPosition());
        telemetryMap.put("current sample", currentSample.name());
        telemetryMap.put("colorsensor", colorSensorEnabled);
        if(colorSensorEnabled) {
//            telemetryMap.put("colorsensor values", colorSensor.getNormalizedColors().red + " " + colorSensor.getNormalizedColors().green + " " + colorSensor.getNormalizedColors().blue);
            telemetryMap.put("colorsensor hsv", "" + HSVasString());
        }
        telemetryMap.put("beater speed", beater.getPower());
        telemetryMap.put("elbow angle target : real", elbow.getTargetAngle() + " : " + elbow.getCurrentAngle());
        telemetryMap.put("wrist angle target : real", wrist.getTargetAngle() + " : " + wrist.getCurrentAngle());
        return telemetryMap;
    }

    @Override
    public String getTelemetryName() {
        return "TRIDENT";
    }
}
