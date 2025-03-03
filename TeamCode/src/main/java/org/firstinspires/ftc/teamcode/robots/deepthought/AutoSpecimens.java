package org.firstinspires.ftc.teamcode.robots.deepthought;

import static org.firstinspires.ftc.teamcode.robots.deepthought.IntoTheDeep_6832.alliance;
import static org.firstinspires.ftc.teamcode.robots.deepthought.IntoTheDeep_6832.field;
import static org.firstinspires.ftc.teamcode.util.utilMethods.futureTime;
import static org.firstinspires.ftc.teamcode.util.utilMethods.isPast;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Pose2d;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.robots.deepthought.field.POI;
import org.firstinspires.ftc.teamcode.robots.deepthought.subsystem.Robot;
import org.firstinspires.ftc.teamcode.robots.deepthought.subsystem.Trident;
import org.firstinspires.ftc.teamcode.robots.deepthought.util.DTPosition;
import org.firstinspires.ftc.teamcode.robots.deepthought.util.TelemetryProvider;

import java.util.LinkedHashMap;
import java.util.Map;

@Config(value = "AA_ITD_Auto_Spec")
public class AutoSpecimens implements TelemetryProvider {
    public static double AUTON_WAIT_TIMER = 1;
    public static int numCycles = 4;
    private Robot robot;
    private HardwareMap hardwareMap;

    //
    public enum AutonState {
        INIT, DRIVE_TO_HIGHBAR, DRIVE_TO_OZONE, OUTTAKE_TO_HIGHBAR, DRIVE_TO_SUB,
    }

    public AutonState autonState = AutonState.INIT;

    @Override
    public Map<String, Object> getTelemetry(boolean debug) {
        Map<String, Object> telemetryMap = new LinkedHashMap<>();
        telemetryMap.put("autonState\t ", autonState);
        telemetryMap.put("autonIndex\t", autonIndex);
        telemetryMap.put("outtakeIndex\t", autonOuttakeIndex);
        telemetryMap.put("outtake timer\t", isPast(autonOuttakeTimer));
        telemetryMap.put("intakeIndex\t", autonShiftIndex);
        return telemetryMap;
    }

    @Override
    public String getTelemetryName() {
        return "AUTOSPEC";
    }

    // autonomous routines

    public static int selectedPath;
    public int allianceMultiplier = 1;


    public static double FIELD_INCHES_PER_GRID = 23.5;
    public static double AUTON_START_DELAY = 0;

    public AutoSpecimens(Robot robot) {
        this.robot = robot;
        this.hardwareMap = this.robot.hardwareMap;
        autonIndex = 0;
    }

    public static int autonIndex;
    public long autonTimer = futureTime(10);
    public long gameTimer;

    public boolean execute(TelemetryPacket packet) {
        if (!alliance.isRed()) {
            allianceMultiplier = -1;
        }
        robot.positionCache.update(new DTPosition(robot.driveTrain.getPose(), robot.trident.getShoulderCurrentPosition(), robot.trident.sampler.slide.getCurrentPosition(), robot.trident.speciMiner.slide.getCurrentPosition()), false);
        switch (autonIndex) { //auton delay
            case 0:
                gameTimer = futureTime(27);
                autonState = AutonState.INIT;
                autonTimer = futureTime(AUTON_START_DELAY);
                autonIndex++;
                break;
            case 1: // travel to hibar field position
                autonState = AutonState.DRIVE_TO_HIGHBAR; // drive to sub
                if (isPast(autonTimer)) {
                    if (robot.driveTrain.strafeToPose(field.hibar.getPose(), packet)) {
                        autonIndex++;
                    }
                }
                break;
            case 2: // set pre latch arm position todo start near very end of drive
                    robot.trident.speciMiner.prelatchHigh();  // preset arm position
                    autonIndex++;

                break;

            case 3: // reserve in case we need to micro adjust field position before latching
                autonTimer = futureTime(1); //enough time to complete latch
                autonIndex++;
                break;
            case 4: // latch specimen
                if (robot.trident.speciMiner.latch()) {
                        robot.resetStates();
                        robot.articulate(Robot.Articulation.MANUAL);
                        autonIndex++;
                    }
                break;
            case 5: // eject - might not be needed?
                if (robot.trident.speciMiner.eject())
                    if(robot.trident.tuck()) autonIndex++;
                break;

            case 6: // back up a bit? in case strafe conflicts with sub
                autonIndex++;
                break;
            case 7: // start sweeping the ground samples
                if (autonShiftSample(field.ground4, packet)) {
                    autonIndex++;
                }
                break;
            case 8:
                if (autonShiftSample(field.ground5, packet)) {
                    autonIndex++;
                }
                break;
            case 9:
                if (autonShiftSample(field.ground6, packet)) {
                    autonIndex++;
                }
                break;
            case 10:
                autonIndex=0;
                return true;
        }
        return false;
    }

    public boolean execSweeping(TelemetryPacket packet) {
        if (!alliance.isRed()) {
            allianceMultiplier = -1;
        }
        robot.positionCache.update(new DTPosition(robot.driveTrain.getPose(), robot.trident.getShoulderCurrentPosition(), robot.trident.sampler.slide.getCurrentPosition(), robot.trident.speciMiner.slide.getCurrentPosition()), false);
        switch (autonIndex) { //auton delay
            case 0:
                resetStates(); // resets all state variables
                gameTimer = futureTime(27);
                autonState = AutonState.INIT;
                autonTimer = futureTime(AUTON_START_DELAY);
                autonIndex++;
                break;
            case 1: // drive to hibar with gripped specimen and latch
                autonState = AutonState.DRIVE_TO_HIGHBAR; // drive to sub
                if (isPast(autonTimer)) {
                    if (driveAndLatch(packet)) {
                        autonIndex=7;
                    }
                }
                break;
            case 7: // start sweeping the ground samples
                if (autonSweepSample(field.sweep1, field.sweep1Oz, packet)) {
                    autonIndex++;
                }
                break;
            case 8:
                if (autonSweepSample(field.sweep2, field.sweep2Oz, packet)) {
                    autonIndex++;
                }
                break;
            case 9:
                if (autonSweepSample(field.sweep3, field.sweep3Oz, packet)) {
                    autonIndex++;
                }
                break;
            case 10:
                autonIndex = 0; //so we can test autons back to back
                return true;
        }
        return false;
    }
    //includes driving to outtake, actual latching, and leaves the robot in outtake position
    public int autonOuttakeIndex = 0;
    public long autonOuttakeTimer = 0;

    public boolean autonSpecimenOuttake(TelemetryPacket packet) {
        switch (autonOuttakeIndex) {
            case 0: // not sure we need another wait here if there is one in execute()
                robot.resetStates();
                autonOuttakeTimer = futureTime(AUTON_WAIT_TIMER);
                autonOuttakeIndex++;
                break;
            case 1: // score the preload alliance sample
                if (isPast(autonOuttakeTimer)) {
                    Trident.enforceSlideLimits = true;
                    robot.articulate(Robot.Articulation.SPECIMINER_OUTTAKE);
                    autonOuttakeTimer = futureTime(1.75);
                    //todo - set Speciminer and Shoulder for hibar prep while driving

                }
                // drive to hibar prep location
                if (robot.driveTrain.strafeToPose(field.hibarPrep.getPose(), packet)) {
                    autonOuttakeIndex++;
                }
                break;
            case 2: //todo keep modifying for specimens
                if (isPast(autonOuttakeTimer)) {
//                    robot.aprilTagRelocalization();
                    autonOuttakeTimer = futureTime(2);
                    autonOuttakeIndex++;
                }
                break;

            case 3:
                robot.aprilTagRelocalization();
                if (isPast(autonOuttakeTimer)) {
                    robot.trident.sampler.servoPower = 0;
                    autonOuttakeIndex = 0;
                    return true;
                }
                break;
        }
        return false;
    }

    // drive to hibar and latch specimen
    // only safe to start from a field position that can direct travel to hibar
    int driveAndLatchIndex = 0;
    double driveAndLatchTimer = 0;
    boolean driveAndLatch(TelemetryPacket packet) {
        switch (driveAndLatchIndex) { //auton delay
            case 0:
                resetStates(); // resets all state variables
                driveAndLatchIndex++;
                break;
            case 1: // travel to hibar field position
                autonState = AutonState.DRIVE_TO_HIGHBAR; // drive to sub
                if (isPast(driveAndLatchTimer)) {
                    if (robot.driveTrain.strafeToPose(field.hibar.getPose(), packet)) {
                        driveAndLatchIndex++;
                    }
                }
                break;
            case 2: // set pre latch arm position todo start near very end of drive
                robot.trident.speciMiner.prelatchHigh();  // preset arm position
                driveAndLatchIndex++;

                break;

            case 3: // reserve in case we need to micro adjust field position before latching
                driveAndLatchIndex++;
                break;
            case 4: // latch specimen
                if (robot.trident.speciMiner.latch()) {
                    driveAndLatchIndex++;
                }
                break;
            case 5: // eject - might not be needed?
                if (robot.trident.speciMiner.eject())
                    if (robot.trident.tuck()) driveAndLatchIndex++;
                break;

            case 6: // back up a bit? in case strafe conflicts with sub
                resetStates();
                return true;
        }
        return false;
    }
        public int autonShiftIndex = 0;
    public int autonShiftTimer = 0;
    int numAttempts = 2;

    //shift a given sample to ozone using the chassis backplate
    public boolean autonShiftSample(POI ground, TelemetryPacket packet) {
        switch (autonShiftIndex) {
            case 0: // drive from hibar or from starting position to safe intermediate
                if (robot.driveTrain.strafeToPose(field.zig.getPose(), packet)) {
                    autonShiftIndex++;
                    //robot.resetStates();
                }
            case 1: // get beyond alliance samples
                if (robot.driveTrain.strafeToPose(field.zag.getPose(), packet)) {
                    autonShiftIndex++;
                    //robot.resetStates();
                }
                break;
            case 2: //drive beyond target sample
                if (robot.driveTrain.strafeToPose(ground.getPose(), packet)) {
                    autonShiftIndex++;
                    //robot.resetStates();
                }
                break;
            case 3: // push to ozone - current X wih a y displacement
                if (robot.driveTrain.strafeToPose(new Pose2d(ground.getPose().position.x, -2.3, 90), packet)) {
                    autonShiftIndex++;
                    //robot.resetStates();
                    robot.articulate(Robot.Articulation.TRAVEL);
                    autonShiftIndex = 0;
                    return true;
                }
                break;
        }
        return false;
    }
    public int autonSweepIndex = 0;
    public int autonSweepTimer = 0;

    //Sweep a given sample to ozone using the chassis backplate
    public boolean autonSweepSample(POI sweepFrom, POI ozone, TelemetryPacket packet) {
        switch (autonSweepIndex) {
            case 0: // set sampler for sweeping over
                if (robot.trident.sampler.sweepConfig(true))
                    autonSweepIndex++;
                break;
            case 1: // drive from hibar or from starting position to sweeping position
                if (robot.driveTrain.strafeToPose(sweepFrom.getPose(), packet)) {
                    if (robot.trident.sampler.sweepConfig(false)) // sampler floats just above floor
                        autonSweepIndex++;
                }
                break;
            case 2: // let's sweep and return
                if (robot.driveTrain.strafeToPose(ozone.getPose(), packet)) {
                    autonSweepIndex++;
                    resetStates(); //be careful with this
                    return true;
                }
                break;
            
        }
        return false;
    }

void resetStates(){
        //do NOT reset autonIndex
        driveAndLatchIndex = 0;
        autonSweepIndex = 0;
        autonShiftIndex = 0;
        robot.resetStates();
}
}
