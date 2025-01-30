package org.firstinspires.ftc.teamcode.robots.deepthought.subsystem;

import static org.firstinspires.ftc.teamcode.robots.csbot.util.Utils.wrapAngle;
import static org.firstinspires.ftc.teamcode.robots.deepthought.IntoTheDeep_6832.alliance;
import static org.firstinspires.ftc.teamcode.robots.deepthought.IntoTheDeep_6832.field;
import static org.firstinspires.ftc.teamcode.robots.deepthought.IntoTheDeep_6832.gameState;
import static org.firstinspires.ftc.teamcode.robots.deepthought.DriverControls.fieldOrientedDrive;
import static org.firstinspires.ftc.teamcode.robots.deepthought.IntoTheDeep_6832.startingPosition;
import static org.firstinspires.ftc.teamcode.util.utilMethods.futureTime;

import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.Vector2d;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDCoefficients;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.VoltageSensor;

import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.teamcode.robots.deepthought.IntoTheDeep_6832;
import org.firstinspires.ftc.teamcode.robots.deepthought.subsystem.old.Sensors;
import org.firstinspires.ftc.teamcode.robots.deepthought.subsystem.samplers.Sampler;
import org.firstinspires.ftc.teamcode.robots.deepthought.util.Constants;
import org.firstinspires.ftc.teamcode.robots.deepthought.util.DTPosition;
import org.firstinspires.ftc.teamcode.robots.deepthought.util.Joint;
import org.firstinspires.ftc.teamcode.robots.deepthought.util.PositionCache;
import org.firstinspires.ftc.teamcode.robots.deepthought.vision.Target;
import org.firstinspires.ftc.teamcode.util.PIDController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Config(value = "0_ITD_Robot")
public class Robot implements Subsystem {

    public static double APRILTAG_Y_OFFSET = 2;
    public static double APRILTAG_X_OFFSET = -26;
    //components and subsystems
    public Subsystem[] subsystems;
    public static Sensors sensors;
    public DriveTrain driveTrain;
//    public VisionProvider visionProviderBack, visionProviderFront;

    public Trident trident;
    public Joint pan; // pan servo for the Limelight
    private Limelight3A limelight; // smart camera
    public static boolean updatePositionCache = false;
    public static boolean visionOn = true;

    public boolean calibrating = false;

    public PositionCache positionCache;
    public DTPosition currPosition;

    public DTPosition fetchedPosition;

    PIDController sampleAlignmentPID;
    PIDCoefficients sampleAlignmentCoefficients = new PIDCoefficients(0.03, 0.04, 0);

    public static double SAMPLE_ALIGN_TARGET_TX = 2.5;
    public static double SAMPLE_ALIGN_TOLERANCE = .5;
    public double PIDError, PIDCorrection;

    //vision variables
    public static boolean visionProviderFinalized = false;
    //    public static int backVisionProviderIndex = 0;
//    public static int frontVisionProviderIndex = 0;
    public double aprilTagRelocalizationX = 0;
    public double aprilTagRelocalizationY = 0;
    //REMOVE
    public Pose2d aprilTagPose = new Pose2d(0, 0, 0);

    private long[] subsystemUpdateTimes;
    private List<LynxModule> hubs = null;
    public HardwareMap hardwareMap;
    private VoltageSensor batteryVoltageSensor;
    public Articulation articulation;
    public List<Target> targets = new ArrayList<Target>();
    public boolean fetched;

    //pan servo
    public static double PAN_START_ANGLE = 145;
    public static int PAN_HOME_POSITION = 2050;
    public static double PAN_PWM_PER_DEGREE = -5.672222222222222;
    public static double PAN_JOINT_SPEED = 120;
    public static double PAN_MIN_ANGLE = -15;
    public static double PAN_MAX_ANGLE = 220;
    public static double PAN_ADJUST_ANGLE = 5;

    public static double PAN_FORWARD = 82;
    public static double PAN_APRILTAG_BASKET = 200;

    public static double panTargetAngle = PAN_FORWARD;


    public enum Articulation {
        MANUAL, SAMPLER_INTAKE, TRAVEL, SAMPLER_OUTTAKE, SPECIMINER_INTAKE, SPECIMINER_WALLTAKE, SAMPLER_PREP, SPECIMINER_OUTTAKE
    }

    public void start() {
        if (gameState.equals(IntoTheDeep_6832.GameState.TELE_OP)) {
            trident.shoulder.setPosition(250);
        }
        limelight.start();
        trident.finalizeTargets();
        field.finalizeField(alliance);
    }
    //end start


    public Robot(HardwareMap hardwareMap, boolean simulated) {
        if (!simulated) {
            this.hardwareMap = hardwareMap;
            hubs = hardwareMap.getAll(LynxModule.class);
            for (LynxModule module : hubs) {
                module.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
            }
            //initialize vision
//            createVisionProviders();

            positionCache = new PositionCache(5);

            // initializing subsystems
            driveTrain = new DriveTrain(hardwareMap, this, false);
            trident = new Trident(hardwareMap, this);
            //sensors = new Sensors(this);

            limelight = hardwareMap.get(Limelight3A.class, "limelight");
            pan = new Joint(hardwareMap, "pan", false, PAN_HOME_POSITION, PAN_PWM_PER_DEGREE, PAN_MIN_ANGLE, PAN_MAX_ANGLE, PAN_START_ANGLE, PAN_JOINT_SPEED);


            subsystems = new Subsystem[]{driveTrain, trident};
            subsystemUpdateTimes = new long[subsystems.length];

            batteryVoltageSensor = hardwareMap.voltageSensor.iterator().next();
            trident.sampler.slideTargetPosition = 0;

            articulation = Articulation.MANUAL;
        }

    }
    //end constructor

    public void resetStates() {
        intakeIndex = 0;
        outtakeIndex = 0;
        for (Subsystem k : subsystems) {
            k.resetStates();
        }
    }

    public double deltaTime = 0;
    long lastTime = 0;

    @Override
    public void update(Canvas fieldOverlay) {
        deltaTime = (System.nanoTime() - lastTime) / 1e9;
        lastTime = System.nanoTime();
        clearBulkCaches(); //ALWAYS FIRST LINE IN UPDATE

        if (updatePositionCache && gameState.isAutonomous()) {
            currPosition = new DTPosition(driveTrain.getPose(), -trident.shoulder.getCurrentPosition(), trident.sampler.slide.getCurrentPosition(), trident.speciMiner.slide.getCurrentPosition());
            positionCache.update(currPosition, false);
        }

        pan.setTargetAngle(panTargetAngle);
        pan.update();

        articulate(articulation);
        driveTrain.updatePoseEstimate();

        drawRobot(fieldOverlay, driveTrain.getPose());
        //update subsystems
        for (int i = 0; i < subsystems.length; i++) {
            Subsystem subsystem = subsystems[i];
            long updateStartTime = System.nanoTime();
            subsystem.update(fieldOverlay);
            subsystemUpdateTimes[i] = System.nanoTime() - updateStartTime;
        }
    }
    //end update

    private static void drawRobot(Canvas c, Pose2d t) {
        final double ROBOT_RADIUS = 8;

        c.setStrokeWidth(1);
        c.strokeCircle(t.position.x, t.position.y, ROBOT_RADIUS);

        Vector2d halfv = t.heading.vec().times(0.5 * ROBOT_RADIUS);
        Vector2d p1 = t.position.plus(halfv);
        Vector2d p2 = p1.plus(halfv);
        c.strokeLine(p1.x, p1.y, p2.x, p2.y);
    }


    public void preloadAllianceSelect() {
        trident.sampler.updateColorSensor();
        if (trident.currentSample == Trident.Sample.RED) {
            alliance = Constants.Alliance.RED;
            startingPosition = startingPosition.isRed() == true ? startingPosition : startingPosition == Constants.Position.START_LEFT_BLUE ? Constants.Position.START_LEFT_RED : Constants.Position.START_RIGHT_RED;
        } else if (trident.currentSample == Trident.Sample.BLUE) {
            alliance = Constants.Alliance.BLUE;
            startingPosition = startingPosition.isRed() == false ? startingPosition : startingPosition == Constants.Position.START_LEFT_RED ? Constants.Position.START_LEFT_BLUE : Constants.Position.START_RIGHT_BLUE;
        }
    }


    public Articulation articulate(Articulation target) {
        articulation = target;
        switch (this.articulation) {
            case MANUAL:
                break;
//            case CALIBRATE:
//                if (calibrate())
//                    articulation = Articulation.MANUAL;
//                break;
            case SAMPLER_INTAKE:
                if (samplerIntake()) articulation = Articulation.MANUAL;
                break;
            case SAMPLER_PREP:
                if (samplerPrep()) articulation = Articulation.MANUAL;
                break;
            case TRAVEL:
                trident.articulate(Trident.Articulation.TUCK);
                if (trident.articulation == Trident.Articulation.MANUAL) {
                    articulation = Articulation.MANUAL;
                }
                break;
            case SAMPLER_OUTTAKE:
                if (samplerOuttake()) articulation = Articulation.MANUAL;
                break;
        }
        return articulation;
    }


    public int intakeIndex = 0;

    public boolean samplerIntake() {
        switch (intakeIndex) {
            case 0:
                // todo this might be at wrong level or ignored
                trident.sampler.articulate(Sampler.Articulation.INTAKE);
                intakeIndex++;
                break;
            case 1:
                if (trident.sampler.articulation == Sampler.Articulation.MANUAL) {
                    intakeIndex = 0;
                    return true;
                }
                break;
        }
        return false;
    }


    public boolean samplerPrep() {
        trident.sampler.articulate(Sampler.Articulation.INTAKE_PREP);
        return true;
    }

    public int outtakeIndex = 0;

    public boolean samplerOuttake() {
        switch (outtakeIndex) {
            case 0:
                Trident.enforceSlideLimits = false; // todo this might be at wrong level or ignored
                trident.sampler.articulate(Sampler.Articulation.OUTTAKE);
                outtakeIndex++;
                break;
            case 1:
                if (trident.sampler.articulation == Sampler.Articulation.MANUAL) {
                    Trident.enforceSlideLimits = true;
                    outtakeIndex = 0;
                    return true;
                }
                break;
        }

        return false;
    }


    @Override
    public void stop() {
        currPosition = new DTPosition(driveTrain.getPose(), -trident.shoulder.getCurrentPosition(), trident.sampler.slide.getCurrentPosition(), trident.speciMiner.slide.getCurrentPosition());
        positionCache.update(currPosition, true);
        for (Subsystem component : subsystems) {
            component.stop();
        }
    }
    //end stop

    @Override
    public Map<String, Object> getTelemetry(boolean debug) {
        Map<String, Object> telemetryMap = new LinkedHashMap<>();
        telemetryMap.put("Articulation", articulation);
        telemetryMap.put("fieldOrientedDrive?", fieldOrientedDrive);
        telemetryMap.put("calibrating", calibrating);
        telemetryMap.put("april tag pose", "(" + aprilTagPose.position.x / 23.5 + ", " + aprilTagPose.position.y / 23.5 + ")");
        telemetryMap.put("MemoryPose", positionCache.readPose());

        telemetryMap.put("pid error", PIDError);
        telemetryMap.put("pid correction", PIDCorrection);
        telemetryMap.put("limelight running?", limelight.isRunning());
        LLStatus status = limelight.getStatus();
        telemetryMap.put("limelight fps, ", status.getFps());
        telemetryMap.put("limelight pipeline", "index: " + status.getPipelineIndex() + " type: " + status.getPipelineType());
        //todo - remove unnecessary telemetry here
        LLResult result = limelight.getLatestResult();
        if (result != null) {
            telemetryMap.put("limelight botpose", result.getBotpose());
            telemetryMap.put("# of limelight detections", result.getDetectorResults().size());
            telemetryMap.put("limelight tx", result.getTx());
        }
        for (int i = 0; i < subsystems.length; i++) {
            String name = subsystems[i].getClass().getSimpleName();
            telemetryMap.put(name + " Update Time", Misc.formatInvariant("%d ms (%d hz)", (int) (subsystemUpdateTimes[i] * 1e-6), (int) (1 / (subsystemUpdateTimes[i] * 1e-9))));
        }


        telemetryMap.put("Delta Time", deltaTime);


        return telemetryMap;
    }
    //end getTelemetry

//    public void createVisionProviders() {
//        try {
//            visionProviderBack = VisionProviders.VISION_PROVIDERS[backVisionProviderIndex].newInstance().setRedAlliance(alliance.isRed());
//            visionProviderFront = VisionProviders.VISION_PROVIDERS[frontVisionProviderIndex].newInstance().setRedAlliance(alliance.isRed());
//        } catch (IllegalAccessException | InstantiationException e) {
//            throw new RuntimeException("Error while instantiating vision provider");
//        }
//    }

    public void clearBulkCaches() {
        for (LynxModule module : hubs)
            module.clearBulkCache();
    }

    public double getVoltage() {
        return batteryVoltageSensor.getVoltage();
    }

    @Override
    public String getTelemetryName() {
        return "ROBOT";
    }

    public void fetchCachedDTPosition() {
        fetchedPosition = positionCache.readPose();
        fetched = fetchedPosition != null;
    }

    public void resetRobotPosFromCache(double loggerTimeoutMinutes, boolean ignoreCache) {
        if (!ignoreCache) {
            fetchCachedDTPosition();
            if (gameState.equals(IntoTheDeep_6832.GameState.TELE_OP) || gameState.equals((IntoTheDeep_6832.GameState.TEST))) {
                int loggerTimeout = (int) (loggerTimeoutMinutes * 60000);
                if (!(System.currentTimeMillis() - fetchedPosition.getTimestamp() > loggerTimeout || ignoreCache)) {
                    //apply cached position
                    driveTrain.setPose(fetchedPosition.getPose());
                    trident.shoulder.setPosition(-fetchedPosition.getShoulderPosition());
                    trident.sampler.slide.setTargetPosition(fetchedPosition.getSlidePosition());
                    trident.speciMiner.slide.setTargetPosition(fetchedPosition.getSlide2Position());
                    trident.shoulder.setDirection(DcMotor.Direction.REVERSE);
                }
            }
        }
    }


    public boolean alignOnSample() {
        limelight.pipelineSwitch(3);
        LLResult llResult;
        if ((llResult = limelight.getLatestResult()) != null) {
            if (llResult.getTx() != 0.0) {
                double targetTx = SAMPLE_ALIGN_TARGET_TX;
                sampleAlignmentPID.setPID(sampleAlignmentCoefficients);
                sampleAlignmentPID.setInput(llResult.getTx());
                sampleAlignmentPID.setSetpoint(targetTx);
                sampleAlignmentPID.setOutputRange(-.8, .8);
                sampleAlignmentPID.setTolerance(SAMPLE_ALIGN_TOLERANCE);
                double correction = sampleAlignmentPID.performPID();
                PIDCorrection = correction;
                PIDError = sampleAlignmentPID.getError();
                if (sampleAlignmentPID.onTarget()) {
                    driveTrain.setDrivePowers(new PoseVelocity2d(new Vector2d(0, 0), 0));
                    return true;
                } else {
                    sampleAlignmentPID.enable();
                    driveTrain.setDrivePowers(new PoseVelocity2d(new Vector2d(0, 0), correction));
                    return false;
                }
            }
        }

        return false;
    }

    //to be called repeatedly until success
    public void aprilTagRelocalization() {
        limelight.pipelineSwitch(2);

        LLResult llResult;
        if ((llResult = limelight.getLatestResult()) != null) {
            //limelight returns everything in meters
            aprilTagPose = new Pose2d(new Vector2d(llResult.getBotpose().getPosition().x * 39.37 + APRILTAG_X_OFFSET, llResult.getBotpose().getPosition().y * 39.37 + APRILTAG_Y_OFFSET), driveTrain.getPose().heading);
            if (llResult.getBotpose().getPosition().x != 0)
                driveTrain.setPose(aprilTagPose);
        }
//            aprilTagRelocalizationX = field.getAprilTagPose(targetTag.id).position.x - targetTag.pose.z * 39.37 - DISTANCE_FROM_CAMERA_TO_CENTER_X;
//            aprilTagRelocalizationY = field.getAprilTagPose(targetTag.id).position.y + targetTag.pose.x * 39.37 - DISTANCE_FROM_CAMERA_TO_CENTER_Y;
//            aprilTagPose = new Pose2d(targetTag.pose.z, targetTag.pose.x, driveTrain.pose.heading.log());
    }


//    public void enableVision() {
//        if (visionOn) {
//            if (!visionProviderFinalized) {
//                createVisionProviders();
//                visionProviderBack.initializeVision(hardwareMap, this, false);
//                visionProviderFront.initializeVision(hardwareMap, this, true);
//                visionProviderFinalized = true;
////
//            }
////            if (frontVision) {
////                visionProviderFront.update(debugTelemetryEnabled);
////            } else visionProviderBack.update(debugTelemetryEnabled);
//        }
//    }


//    public ArrayList<AprilTagDetection> getAprilTagDetections() {
//        if (visionOn) {
//            if (visionProviderFinalized) {
//                return ((AprilTagProvider) visionProviderBack).getDetections();
//            }
//        }
//        return null;
//    }

//    public void switchVisionProviders() {
//        visionProviderBack.shutdownVision();
//        visionProviderFront.shutdownVision();
//        if (backVisionProviderIndex == 2) {
//            //switch to AprilTags
//            backVisionProviderIndex = 0;
//            visionProviderFinalized = false;
//
//        } else if (backVisionProviderIndex == 0) {
//            //switch back to ColorBlob
//            backVisionProviderIndex = 2;
//            visionProviderFinalized = false;
//        }
//    }

//    public static int calibrateIndex = 0;
//    public long calibrateTimer = 0;
//    public boolean calibrate() {
//        calibrating = true;
//        switch (calibrateIndex) {
//            case 1:
//                calibrateTimer = futureTime(1);
//                trident.shoulder.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//                trident.shoulder.setPower(-.5);
//                calibrateIndex++;
//
//            case 2:
//                if(Trident.SHOULDER_CALIBRATE_ENCODER == trident.shoulder.getCurrentPosition() && isPast(calibrateTimer)) {
//                    calibrateIndex++;
//                }
//                else {
//                    Trident.SHOULDER_CALIBRATE_ENCODER = trident.shoulder.getCurrentPosition();
//                }
//                break;
//            case 3:
//                trident.shoulder.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//                trident.shoulder.setTargetPosition(2450);
//                trident.shoulder.setPower(1);
//                trident.shoulder.setMode(DcMotor.RunMode.RUN_TO_POSITION);
//                calibrateIndex++;
//
//            case 4:
//                if(withinError(trident.shoulder.getCurrentPosition(), 2450, 3)) {
//                    trident.shoulder.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//                    trident.shoulder.setDirection(DcMotorSimple.Direction.REVERSE);
//                    trident.shoulder.setPower(1);
//                    trident.shoulder.setVelocity(400);
//                    trident.shoulder.setMode(DcMotor.RunMode.RUN_TO_POSITION);
//                    calibrateIndex++;
//                }
//                break;
//            case 5:
//                trident.sampler.shoulderTargetPosition = 800;
//                calibrateIndex++;
//                calibrating = false;
//                return true;
//
//
//        }
//        return false;
//    }

}
