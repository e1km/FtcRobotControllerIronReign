package org.firstinspires.ftc.teamcode.robots.deepthought;

import static org.firstinspires.ftc.teamcode.util.utilMethods.futureTime;
import static org.firstinspires.ftc.teamcode.util.utilMethods.isPast;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.robots.deepthought.field.Field;
import org.firstinspires.ftc.teamcode.robots.deepthought.subsystem.Robot;
import org.firstinspires.ftc.teamcode.robots.deepthought.util.Constants;
import org.firstinspires.ftc.teamcode.robots.deepthought.util.DTPosition;
import org.firstinspires.ftc.teamcode.robots.deepthought.util.TelemetryProvider;

import java.util.LinkedHashMap;
import java.util.Map;


@Config(value = "AA_CS_Auton")
public class Autonomous implements TelemetryProvider {
    public static int numCycles = 4;
    private Robot robot;
    private HardwareMap hardwareMap;
//
    public enum AutonState {
        INIT,
        DRIVE_TO_BASKET,
        OUTTAKE_TO_BASKET,
        DRIVE_TO_SUB,

    }


    public AutonState autonState = AutonState.INIT;

    @Override
    public Map<String, Object> getTelemetry(boolean debug) {
        Map<String, Object> telemetryMap = new LinkedHashMap<>();
        telemetryMap.put("autonState\t ", autonState);
        telemetryMap.put("auton index\t", autonIndex);
        telemetryMap.put("num cycles\t", numCycles);
        telemetryMap.put("selectedPath\t", selectedPath);
        return telemetryMap;
    }

    @Override
    public String getTelemetryName() {
        return "AUTONOMOUS";
    }

    // autonomous routines

    public static int selectedPath;


    public static double FIELD_INCHES_PER_GRID = 23.5;
    public static double AUTON_START_DELAY = 0;

    public Autonomous(Robot r) {
        robot = r;
        this.hardwareMap = robot.hardwareMap;
        autonIndex = 0;
    }

    public static int autonIndex;
    public long autonTimer = futureTime(10);

    public boolean execute(TelemetryPacket packet, Field field) {
        switch (autonIndex) {
            case 0:
                autonState = AutonState.INIT;
                robot.positionCache.update(new DTPosition(robot.driveTrain.pose), true);
                autonTimer = futureTime(AUTON_START_DELAY);
                autonIndex++;
                break;
            case 1:
                if (isPast(autonTimer)) {
                    autonState = AutonState.DRIVE_TO_BASKET;
                    numCycles--;
                    autonIndex++;
                }
                break;
            case 2:
                if (robot.driveTrain.strafeToPose(Field.P2D(-2.5, -2.5, 90), packet)) {
//                    autonState = AutonState.DRIVE_TO_SUB;
                    return true;
//                    autonIndex++;
                }
                break;
            case 3:
                if (robot.driveTrain.strafeToPose(field.subAccess.pose, packet)) {
                    autonState = AutonState.DRIVE_TO_BASKET;
                    autonIndex++;
                }
                break;
            case 4:
                if (robot.driveTrain.strafeToPose(field.subAccess.pose, packet)) {
                    autonState = AutonState.DRIVE_TO_BASKET;
                    if(numCycles == 0)
                        autonIndex++;
                    else {
                        numCycles --;
                        autonIndex = 2;
                    }
                }
                break;
            case 5:
                break;
            case 6:
                break;
        }
        return false;
    }


}
