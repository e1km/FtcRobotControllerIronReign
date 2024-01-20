package org.firstinspires.ftc.teamcode.robots.csbot.subsystem;

import static org.firstinspires.ftc.teamcode.robots.csbot.CenterStage_6832.alliance;
import static org.firstinspires.ftc.teamcode.robots.csbot.CenterStage_6832.gameState;
import static org.firstinspires.ftc.teamcode.robots.csbot.CenterStage_6832.robot;
import static org.firstinspires.ftc.teamcode.robots.csbot.DriverControls.fieldOrientedDrive;
import static org.firstinspires.ftc.teamcode.util.utilMethods.futureTime;

import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.Vector2d;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.VoltageSensor;

import org.checkerframework.checker.units.qual.A;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.teamcode.robots.csbot.CenterStage_6832;
import org.firstinspires.ftc.teamcode.robots.csbot.util.CSPosition;
import org.firstinspires.ftc.teamcode.robots.csbot.util.Constants;
import org.firstinspires.ftc.teamcode.robots.csbot.util.PositionCache;
import org.firstinspires.ftc.teamcode.robots.csbot.vision.Target;
import org.firstinspires.ftc.teamcode.robots.csbot.vision.VisionProvider;
import org.firstinspires.ftc.teamcode.robots.csbot.vision.VisionProviders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Config(value = "AA_CSRobot")
public class  Robot implements Subsystem {

    //components and subsystems
    public Subsystem[] subsystems;
    public DriveTrain driveTrain;
    public Skyhook skyhook;
    public Intake intake;
    public VisionProvider visionProviderBack = null;
    public static boolean visionOn = true;
    public Outtake outtake;
    //TODO - create a field
//    public Field field;

    public static boolean updatePositionCache = false;
    public PositionCache positionCache;
    public CSPosition currPosition;

    public CSPosition fetchedPosition;

    //vision variables
    public boolean visionProviderFinalized = false;
    public static int visionProviderIndex = 2;

    private long[] subsystemUpdateTimes;
    private final List<LynxModule> hubs;
    public HardwareMap hardwareMap;
    private VoltageSensor batteryVoltageSensor;
    public Articulation articulation;
    public List<Target> targets = new ArrayList<Target>();
    public boolean fetched;
    public boolean selfDriving = true;

    public enum Articulation {
        //beater bar, drivetrain, drone launcher, outtake
        MANUAL,
        AUTON,
        CALIBRATE,
        BACKDROP_PREP,
        BACKDROP,
        FOLD,
        INGEST,
        UNFOLD,
        HANG,
        PREP_FOR_HANG,
        LAUNCH_DRONE,
        TRAVEL_FROM_BACKDROP,
        TRAVEL_FROM_INGEST,
        TRAVEL

    }

    public void start() {
        skyhook.articulate(Skyhook.Articulation.GAME);
        //TODO - articulate starting position
        if (gameState.isAutonomous()) {
            intake.setAngle(1600);
        }
    }
    //end start


    public Robot(HardwareMap hardwareMap, boolean simulated) {
        this.hardwareMap = hardwareMap;
        hubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule module : hubs) {
            module.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
        }
        //initialize vision
        createVisionProvider();

        positionCache = new PositionCache(5);

        // initializing subsystems
        driveTrain = new DriveTrain(hardwareMap, this, simulated);
        intake = new Intake(hardwareMap, this);
        outtake = new Outtake(hardwareMap, this);
        skyhook = new Skyhook(hardwareMap, this);


        subsystems = new Subsystem[]{driveTrain, intake, outtake, skyhook};
        subsystemUpdateTimes = new long[subsystems.length];

        batteryVoltageSensor = hardwareMap.voltageSensor.iterator().next();

        articulation = Robot.Articulation.MANUAL;

//        field = new Field(true);
    }
    //end constructor

    public double deltaTime = 0;
    long lastTime = 0;

    @Override
    public void update(Canvas fieldOverlay) {
        deltaTime = (System.nanoTime() - lastTime) / 1e9;
        lastTime = System.nanoTime();
        clearBulkCaches(); //ALWAYS FIRST LINE IN UPDATE

        if (updatePositionCache && gameState.isAutonomous()) {
            currPosition = new CSPosition(driveTrain.pose, skyhook.getSkyhookLeftTicksCurrent(), skyhook.getSkyhookRightTicksCurrent());
            positionCache.update(currPosition, false);
        }

        articulate(articulation);
        //TODO - DELETE
        driveTrain.updatePoseEstimate();

        drawRobot(fieldOverlay, driveTrain.pose);

        //update subsystems
        for (int i = 0; i < subsystems.length; i++) {
            Subsystem subsystem = subsystems[i];
            long updateStartTime = System.nanoTime();
            subsystem.update(fieldOverlay);
            subsystemUpdateTimes[i] = System.nanoTime() - updateStartTime;
        }
    }
    //end update

    public void updateVision() {
        if (visionOn) {
            if (!visionProviderFinalized) {
                createVisionProvider();
                visionProviderBack.initializeVision(hardwareMap, this);
                visionProviderFinalized = true;

            }
            visionProviderBack.update();
        }
    }

    private static void drawRobot(Canvas c, Pose2d t) {
        final double ROBOT_RADIUS = 9;

        c.setStrokeWidth(1);
        c.strokeCircle(t.position.x, t.position.y, ROBOT_RADIUS);

        Vector2d halfv = t.heading.vec().times(0.5 * ROBOT_RADIUS);
        Vector2d p1 = t.position.plus(halfv);
        Vector2d p2 = p1.plus(halfv);
        c.strokeLine(p1.x, p1.y, p2.x, p2.y);
    }

    public void switchVisionProviders() {
        visionProviderBack.shutdownVision();
        if (visionProviderIndex == 2) {
            //switch to AprilTags
            visionProviderIndex = 0;
            visionProviderFinalized = false;

        } else if (visionProviderIndex == 0) {
            //switch back to ColorBlob
            visionProviderIndex = 2;
            visionProviderFinalized = false;
        }
    }

    public void fetchCachedCSPosition() {
        fetchedPosition = positionCache.readPose();
        fetched = fetchedPosition != null;
    }

    public void resetRobotPosFromCache(double loggerTimeoutMinutes, boolean ignoreCache) {
        if(!ignoreCache) {
            fetchCachedCSPosition();
            if (gameState.equals(CenterStage_6832.GameState.TELE_OP) || gameState.equals((CenterStage_6832.GameState.TEST))) {
                int loggerTimeout = (int) (loggerTimeoutMinutes * 60000);
                if (!(System.currentTimeMillis() - fetchedPosition.getTimestamp() > loggerTimeout || ignoreCache)) {
                    //apply cached position
//                    driveTrain.pose = fetchedPosition.getPose();
                }
            }
        }
    }

    public static int initPositionIndex = 0;
    public long initPositionTimer;

    public void initPosition() {
        switch (initPositionIndex) {
            case 0:
                initPositionTimer = futureTime(1);
                initPositionIndex++;
                break;
            case 1:
                intake.setAngle(Intake.ANGLE_MIN);
//                if(isPast(initPositionTimer)) {
//                    initPositionTimer = futureTime(1);
//                    initPositionIndex ++;
//                }
                break;
            case 2:
                outtake.slideTargetPosition = Outtake.UNTUCK_SLIDE_POSITION;
//                if (isPast(initPositionTimer)) {
//                    initPositionTimer = futureTime(1);
//                    initPositionIndex ++;
//                }
                break;
            case 3:
                outtake.setTargetAngle(Outtake.FLIPPER_START_ANGLE);
                //                if (isPast(initPositionTimer)) {
//                    initPositionTimer = futureTime(1);
//                    initPositionIndex ++;
//                }
                break;
            case 4:
                outtake.slideTargetPosition = 0;
                break;
            case 5:
                //todo load cached skyhook positions
                //this is the only way to work across power cycles until we incorporate a limit switch calibration
                skyhook.skyhookLeft.setPosition(0);
                skyhook.skyhookRight.setPosition(0);
                skyhook.articulate(Skyhook.Articulation.INIT);
                break;
            case 6:
                intake.articulate(Intake.Articulation.INIT);
        }
    }

    public Articulation articulate(Articulation target) {
        articulation = target;
        switch (this.articulation) {
            case MANUAL:
                break;
            case TRAVEL:
//                robot.outtake.articulate(Outtake.Articulation.TRAVEL);
                cleanArticulations();
                break;
            case BACKDROP:
                break;
            case CALIBRATE:
                //TODO - WRITE A CALIBRATION ROUTINE
                break;
            case INGEST:
                if (Ingest()) {
                    articulation = Articulation.TRAVEL_FROM_INGEST;
                }
                break;
            case HANG:
                intake.articulate(Intake.Articulation.HANG);
                skyhook.articulate(Skyhook.Articulation.HANG);
                break;
            case PREP_FOR_HANG:
                skyhook.articulate(Skyhook.Articulation.PREP_FOR_HANG);
                outtake.articulate(Outtake.Articulation.INGEST_FROM_TRAVEL);
                articulation = Articulation.TRAVEL;
                break;
            case LAUNCH_DRONE:
                skyhook.articulate(Skyhook.Articulation.LAUNCH);
                articulation = Articulation.MANUAL;
                break;
            case TRAVEL_FROM_INGEST:
                intake.articulate(Intake.Articulation.TRAVEL);
                if (!(outtake.articulation==Outtake.Articulation.TRAVEL)) {
                    outtake.articulate(Outtake.Articulation.TRAVEL_FROM_INGEST);
                }
                break;
            case TRAVEL_FROM_BACKDROP:
                //assume intake is already in travel
                outtake.articulate(Outtake.Articulation.TRAVEL_FROM_BACKDROP);
                articulation = Articulation.TRAVEL;
                break;
            case BACKDROP_PREP:
                intake.articulate(Intake.Articulation.TRAVEL);
                outtake.articulate(Outtake.Articulation.BACKDROP_PREP);
                articulation = Articulation.BACKDROP;
                break;
        }
        return articulation;
    }

    public void toggleBackdropPrep(){
        if(articulation.equals(Articulation.BACKDROP)){
            articulation = Articulation.TRAVEL_FROM_BACKDROP;
        }
        else{
            articulation = Articulation.BACKDROP_PREP;
        }
    }

    @Override
    public void stop() {
        currPosition = new CSPosition(driveTrain.pose, skyhook.getSkyhookLeftTicksCurrent(), skyhook.getSkyhookRightTicksCurrent());
        positionCache.update(currPosition, true);
        for (Subsystem component : subsystems) {
            component.stop();
        }
    }
    //end stop

    public void cleanArticulations() {
        if(articulation == Articulation.TRAVEL) {
            ingestStage = 0;
            if(intake.articulation == Intake.Articulation.TRAVEL)
                intake.cleanArticulations();
            if(outtake.articulation == Outtake.Articulation.TRAVEL)
                outtake.cleanArticulations();
        }
    }


    public static int ingestStage = 0;
    public long ingestTimer = 0;

    public boolean Ingest() {
        switch (ingestStage) {
            case 0:
                outtake.articulate(Outtake.Articulation.INGEST_FROM_TRAVEL);
                ingestStage++;
            case 1: //wait for outake to dock before proceeding
                if (outtake.articulation == Outtake.Articulation.MANUAL) {
                    //intake can start eating
                    intake.articulate(Intake.Articulation.INGEST);
                    ingestStage++;
                }
                break;
            case 2:  //wait until Intake ingest and swallow are done
                if (!intake.isEating()) ingestStage++;
                break;
            case 3:
                ingestStage=0;
                return true;

        }
        return false;
    }

    public void enterTravel() {
        if(articulation.equals(Articulation.BACKDROP)) {
            articulation = Articulation.TRAVEL_FROM_BACKDROP;
        }
        if(articulation.equals(Articulation.INGEST)) {
            articulation = Articulation.TRAVEL_FROM_INGEST;
        }
        else{
            articulation = Articulation.TRAVEL;
        }
    }

    @Override
    public Map<String, Object> getTelemetry(boolean debug) {
        Map<String, Object> telemetryMap = new LinkedHashMap<>();
        telemetryMap.put("Articulation", articulation);
        telemetryMap.put("fieldOrientedDrive?", fieldOrientedDrive);
        telemetryMap.put("wingIntakeIndex", ingestStage);
        telemetryMap.put("initPositionIndex", initPositionIndex);
//        telemetryMap.put("MemoryPose", positionCache.readPose());
        for (int i = 0; i < subsystems.length; i++) {
            String name = subsystems[i].getClass().getSimpleName();
            telemetryMap.put(name + " Update Time", Misc.formatInvariant("%d ms (%d hz)", (int) (subsystemUpdateTimes[i] * 1e-6), (int) (1 / (subsystemUpdateTimes[i] * 1e-9))));
        }


        telemetryMap.put("Delta Time", deltaTime);


        return telemetryMap;
    }
    //end getTelemetry

    public void createVisionProvider() {
        try {
            visionProviderBack = VisionProviders.VISION_PROVIDERS[visionProviderIndex].newInstance().setRedAlliance(alliance==Constants.Alliance.RED);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Error while instantiating vision provider");
        }
    }

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
}
