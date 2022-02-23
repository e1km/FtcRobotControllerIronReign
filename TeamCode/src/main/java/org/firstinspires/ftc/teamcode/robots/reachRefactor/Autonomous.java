package org.firstinspires.ftc.teamcode.robots.reachRefactor;

import com.acmerobotics.roadrunner.geometry.Vector2d;

import org.firstinspires.ftc.teamcode.robots.reachRefactor.subsystem.Crane;
import org.firstinspires.ftc.teamcode.robots.reachRefactor.subsystem.DriveTrain;
import org.firstinspires.ftc.teamcode.robots.reachRefactor.subsystem.Robot;
import org.firstinspires.ftc.teamcode.robots.reachRefactor.trajectorysequence.TrajectorySequence;
import org.firstinspires.ftc.teamcode.robots.reachRefactor.util.Constants;
import org.firstinspires.ftc.teamcode.robots.reachRefactor.util.Utils;
import org.firstinspires.ftc.teamcode.robots.reachRefactor.vision.VisionProvider;
import org.firstinspires.ftc.teamcode.robots.reachRefactor.vision.VisionProviders;
import org.firstinspires.ftc.teamcode.statemachine.Stage;
import org.firstinspires.ftc.teamcode.statemachine.StateMachine;
import org.firstinspires.ftc.teamcode.robots.reachRefactor.trajectorysequence.TrajectorySequenceBuilder;

import java.util.function.Function;

public class Autonomous {
    public VisionProvider visionProvider;
    private Robot robot;

    // autonomous routines
    private StateMachine blueUp, redUp, blueDown, redDown, blueUpLinear, redUpLinear, blueDownLinear, redDownLinear, blueUpSimple, redUpSimple, blueDownSimple, redDownSimple;
    // misc. routines
    public StateMachine backAndForth, square, turn, lengthTest, diagonalTest;

    public Autonomous(Robot robot) {
        this.robot = robot;
    }

    public StateMachine getStateMachine(Constants.Position startingPosition, boolean spline) {
        if(spline)
            switch(startingPosition) {
                case START_BLUE_UP:
                    return blueUp;
                case START_RED_UP:
                    return redUp;
                case START_BLUE_DOWN:
                    return blueDown;
                case START_RED_DOWN:
                    return redDown;
            }
        else
            switch(startingPosition) {
                case START_BLUE_UP:
                    return blueUpLinear;
                case START_RED_UP:
                    return redUpLinear;
                case START_BLUE_DOWN:
                    return blueDownLinear;
                case START_RED_DOWN:
                    return redDownLinear;
            }
        return null;
    }

    public StateMachine getStateMachineSimple(Constants.Position startingPosition) {
        switch(startingPosition) {
            case START_BLUE_UP:
                return blueUpSimple;
            case START_RED_UP:
                return redUpSimple;
            case START_BLUE_DOWN:
                return blueDownSimple;
            case START_RED_DOWN:
                return redDownSimple;
        }
        return null;
    }

    private StateMachine trajectorySequenceToStateMachine(Function<TrajectorySequenceBuilder, TrajectorySequenceBuilder> function) {
        return Utils.getStateMachine(new Stage())
                .addSingleState(() -> {
                    robot.driveTrain.followTrajectorySequenceAsync(
                        function.apply(
                            robot.driveTrain.trajectorySequenceBuilder(
                                    robot.driveTrain.getPoseEstimate()
                            )
                        )
                        .build()
                    );
                })
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .build();
    }

    private StateMachine trajectorySequenceToStateMachine(TrajectorySequence trajectorySequence) {
        return Utils.getStateMachine(new Stage())
                .addSingleState(() -> {
                    robot.driveTrain.followTrajectorySequenceAsync(
                            trajectorySequence
                    );
                })
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .build();
    }

    public void build() {
        //----------------------------------------------------------------------------------------------
        // Misc. Routines
        //----------------------------------------------------------------------------------------------
        
        TrajectorySequence backAndForthSequence =
                robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                        .back(48)
                        .forward(48)
                        .build();
        backAndForth = trajectorySequenceToStateMachine(backAndForthSequence);

        TrajectorySequence squareSequence =
                robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                        .back(24)
                        .turn(Math.toRadians(-90))
                        .back(24)
                        .turn(Math.toRadians(-90))
                        .back(24)
                        .turn(Math.toRadians(-90))
                        .back(24)
                        .turn(Math.toRadians(-90))
                        .build();
        square = trajectorySequenceToStateMachine(squareSequence);

        TrajectorySequence turnSequence =
                robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                        .turn(Math.toRadians(90))
                        .turn(Math.toRadians(90))
                        .turn(Math.toRadians(90))
                        .turn(Math.toRadians(90))
                        .build();
        turn = trajectorySequenceToStateMachine(turnSequence);

        lengthTest = Utils.getStateMachine(new Stage())
                .addState(() -> {
                    robot.driveTrain.setChassisLengthMode(DriveTrain.ChassisLengthMode.SWERVE);
                    robot.driveTrain.setChassisLength(robot.driveTrain.getTargetChassisLength() + 12);
                    return robot.driveTrain.chassisLengthOnTarget();
                })
                .addSingleState(() -> {
                    robot.driveTrain.followTrajectorySequenceAsync(
                            robot.driveTrain.trajectorySequenceBuilder(
                                    robot.driveTrain.getPoseEstimate()
                            )
                                    .forward(12)
                                    .build()
                    );
                })
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addState(() -> {
                    robot.driveTrain.setChassisLengthMode(DriveTrain.ChassisLengthMode.DIFF);
                    robot.driveTrain.setChassisLength(robot.driveTrain.getTargetChassisLength() - 12);
                    return robot.driveTrain.chassisLengthOnTarget();
                })
                .build();

        //----------------------------------------------------------------------------------------------
        // Spline Routines
        //----------------------------------------------------------------------------------------------

        blueUp = Utils.getStateMachine(new Stage())
                .addSingleState(() -> {
                    robot.driveTrain.followTrajectorySequenceAsync(
                            robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                            .back(33.67)
                            .build()
                    );
                })
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addMineralState(
                        () -> visionProvider.getMostFrequentPosition().getIndex(),
                        () -> { robot.crane.articulate(Crane.Articulation.LOWEST_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.MIDDLE_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.HIGH_TIER); return true; }
                )
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() -> robot.crane.turret.setTargetHeading(90))
                .addState(() -> robot.turret.isTurretNearTarget())
                .addTimedState(1.5f, () -> robot.crane.dump(), () -> robot.crane.articulate(Crane.Articulation.HOME))
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() ->
                    robot.driveTrain.followTrajectorySequenceAsync(
                            robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                    .turn(-Math.toRadians(60))
                                    .forward(50)
                                    .build()
                    )
                )
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addSingleState(() -> robot.crane.articulate(Crane.Articulation.TRANSFER))
                .build();
        redUp = Utils.getStateMachine(new Stage())
                .addSingleState(() -> {
                    robot.driveTrain.followTrajectorySequenceAsync(
                            robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                    .back(33.67)
                                    .build()
                    );
                })
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addMineralState(
                        () -> visionProvider.getMostFrequentPosition().getIndex(),
                        () -> { robot.crane.articulate(Crane.Articulation.LOWEST_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.MIDDLE_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.HIGH_TIER); return true; }
                )
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() -> robot.crane.turret.setTargetHeading(-90))
                .addState(() -> robot.turret.isTurretNearTarget())
                .addTimedState(1.5f, () -> robot.crane.dump(), () -> robot.crane.articulate(Crane.Articulation.HOME))
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() ->
                        robot.driveTrain.followTrajectorySequenceAsync(
                                robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                        .turn(Math.toRadians(60))
                                        .forward(50)
                                        .build()
                        )
                )
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addSingleState(() -> robot.crane.articulate(Crane.Articulation.TRANSFER))
                .build();

        blueDown = Utils.getStateMachine(new Stage())
                .addSingleState(() -> {
                    robot.driveTrain.followTrajectorySequenceAsync(
                            robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                    .back(33.67)
                                    .build()
                    );
                })
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addMineralState(
                        () -> visionProvider.getMostFrequentPosition().getIndex(),
                        () -> { robot.crane.articulate(Crane.Articulation.LOWEST_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.MIDDLE_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.HIGH_TIER); return true; }
                )
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() -> robot.crane.turret.setTargetHeading(-90))
                .addState(() -> robot.turret.isTurretNearTarget())
                .addTimedState(1.5f, () -> robot.crane.dump(), () -> robot.crane.articulate(Crane.Articulation.HOME))
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() ->
                        robot.driveTrain.followTrajectorySequenceAsync(
                                robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                        .setReversed(true)
                                        .splineTo(new Vector2d(-24, 0), Math.toRadians(0))
                                        .splineTo(new Vector2d(12, 12), Math.toRadians(45))
                                        .back(50)
                                        .turn(Math.toRadians(180))
                                        .build()
                        )
                )
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addSingleState(() -> robot.crane.articulate(Crane.Articulation.TRANSFER))
                .build();

        redDown = Utils.getStateMachine(new Stage())
                .addSingleState(() -> {
                    robot.driveTrain.followTrajectorySequenceAsync(
                            robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                    .back(33.67)
                                    .build()
                    );
                })
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addMineralState(
                        () -> visionProvider.getMostFrequentPosition().getIndex(),
                        () -> { robot.crane.articulate(Crane.Articulation.LOWEST_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.MIDDLE_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.HIGH_TIER); return true; }
                )
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() -> robot.crane.turret.setTargetHeading(90))
                .addState(() -> robot.turret.isTurretNearTarget())
                .addTimedState(1.5f, () -> robot.crane.dump(), () -> robot.crane.articulate(Crane.Articulation.HOME))
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() ->
                        robot.driveTrain.followTrajectorySequenceAsync(
                                robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                        .setReversed(true)
                                        .splineTo(new Vector2d(-24, 0), Math.toRadians(0))
                                        .splineTo(new Vector2d(12, -12), Math.toRadians(315))
                                        .back(50)
                                        .turn(Math.toRadians(180))
                                        .build()
                        )
                )
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addSingleState(() -> robot.crane.articulate(Crane.Articulation.TRANSFER))
                .build();

        //----------------------------------------------------------------------------------------------
        // Linear Routines
        //----------------------------------------------------------------------------------------------

        blueUpLinear = Utils.getStateMachine(new Stage())
                .addSingleState(() -> {
                    robot.driveTrain.followTrajectorySequenceAsync(
                            robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                    .back(33.67)
                                    .build()
                    );
                })
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addMineralState(
                        () -> visionProvider.getMostFrequentPosition().getIndex(),
                        () -> { robot.crane.articulate(Crane.Articulation.LOWEST_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.MIDDLE_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.HIGH_TIER); return true; }
                )
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() -> robot.crane.turret.setTargetHeading(90))
                .addState(() -> robot.turret.isTurretNearTarget())
                .addTimedState(1.5f, () -> robot.crane.dump(), () -> robot.crane.articulate(Crane.Articulation.HOME))
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() ->
                        robot.driveTrain.followTrajectorySequenceAsync(
                                robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                        .turn(-Math.toRadians(60))
                                        .forward(50)
                                        .build()
                        )
                )
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addSingleState(() -> robot.crane.articulate(Crane.Articulation.TRANSFER))
                .build();
        redUpLinear = Utils.getStateMachine(new Stage())
                .addSingleState(() -> {
                    robot.driveTrain.followTrajectorySequenceAsync(
                            robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                    .back(33.67)
                                    .build()
                    );
                })
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addMineralState(
                        () -> visionProvider.getMostFrequentPosition().getIndex(),
                        () -> { robot.crane.articulate(Crane.Articulation.LOWEST_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.MIDDLE_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.HIGH_TIER); return true; }
                )
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() -> robot.crane.turret.setTargetHeading(-90))
                .addState(() -> robot.turret.isTurretNearTarget())
                .addTimedState(1.5f, () -> robot.crane.dump(), () -> robot.crane.articulate(Crane.Articulation.HOME))
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() ->
                        robot.driveTrain.followTrajectorySequenceAsync(
                                robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                        .turn(Math.toRadians(60))
                                        .forward(50)
                                        .build()
                        )
                )
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addSingleState(() -> robot.crane.articulate(Crane.Articulation.TRANSFER))
                .build();

        blueDownLinear = Utils.getStateMachine(new Stage())
                .addSingleState(() -> {
                    robot.driveTrain.followTrajectorySequenceAsync(
                            robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                    .back(33.67)
                                    .build()
                    );
                })
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addMineralState(
                        () -> visionProvider.getMostFrequentPosition().getIndex(),
                        () -> { robot.crane.articulate(Crane.Articulation.LOWEST_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.MIDDLE_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.HIGH_TIER); return true; }
                )
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() -> robot.crane.turret.setTargetHeading(-90))
                .addState(() -> robot.turret.isTurretNearTarget())
                .addTimedState(1.5f, () -> robot.crane.dump(), () -> robot.crane.articulate(Crane.Articulation.HOME))
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() ->
                        robot.driveTrain.followTrajectorySequenceAsync(
                                robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                        .back(38.33)
                                        .turn(-Math.toRadians(90))
                                        .forward(48)
                                        .turn(Math.toRadians(90))
                                        .forward(36)
                                        .turn(-Math.toRadians(90))
                                        .forward(36)
                                        .build()
                        )
                )
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addSingleState(() -> robot.crane.articulate(Crane.Articulation.TRANSFER))
                .build();

        redDownLinear = Utils.getStateMachine(new Stage())
                .addSingleState(() -> {
                    robot.driveTrain.followTrajectorySequenceAsync(
                            robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                    .back(33.67)
                                    .build()
                    );
                })
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addMineralState(
                        () -> visionProvider.getMostFrequentPosition().getIndex(),
                        () -> { robot.crane.articulate(Crane.Articulation.LOWEST_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.MIDDLE_TIER); return true; },
                        () -> { robot.crane.articulate(Crane.Articulation.HIGH_TIER); return true; }
                )
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() -> robot.crane.turret.setTargetHeading(90))
                .addState(() -> robot.turret.isTurretNearTarget())
                .addTimedState(1.5f, () -> robot.crane.dump(), () -> robot.crane.articulate(Crane.Articulation.HOME))
                .addState(() -> robot.crane.getArticulation() == Crane.Articulation.MANUAL)
                .addSingleState(() ->
                        robot.driveTrain.followTrajectorySequenceAsync(
                                robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                                        .back(38.33)
                                        .turn(Math.toRadians(90))
                                        .forward(48)
                                        .turn(-Math.toRadians(90))
                                        .forward(36)
                                        .turn(Math.toRadians(90))
                                        .forward(36)
                                        .build()
                        )
                )
                .addState(() -> !robot.driveTrain.trajectorySequenceRunner.isBusy())
                .addSingleState(() -> robot.crane.articulate(Crane.Articulation.TRANSFER))
                .build();

        //----------------------------------------------------------------------------------------------
        // Simple Routines (traditionally walk of fame)
        //----------------------------------------------------------------------------------------------

        blueUpSimple = Utils.getStateMachine(new Stage())
                .addState(() -> robot.crane.articulate(Crane.Articulation.AUTON_REACH_RIGHT))
                .addTimedState(.5f, ()->{}, () -> {})
                .addState(() -> robot.articulate(Robot.Articulation.DUMP_AND_SET_CRANE_FOR_TRANSFER))
                .build();

        redUpSimple = Utils.getStateMachine(new Stage())
                .addState(() -> robot.crane.articulate(Crane.Articulation.AUTON_REACH_LEFT))
                .addTimedState(.5f, ()->{}, () -> {})
                .addState(() -> robot.articulate(Robot.Articulation.DUMP_AND_SET_CRANE_FOR_TRANSFER))
                .build();

        blueDownSimple = Utils.getStateMachine(new Stage())
                .addState(() -> robot.crane.articulate(Crane.Articulation.AUTON_REACH_LEFT))
                .addTimedState(.5f, ()->{}, () -> {})
                .addState(() -> robot.articulate(Robot.Articulation.DUMP_AND_SET_CRANE_FOR_TRANSFER))
                .build();

        redDownSimple = Utils.getStateMachine(new Stage())
                .addState(() -> robot.crane.articulate(Crane.Articulation.AUTON_REACH_RIGHT))
                .addTimedState(.5f, ()->{}, () -> {})
                .addState(() -> robot.articulate(Robot.Articulation.DUMP_AND_SET_CRANE_FOR_TRANSFER))
                .build();

        TrajectorySequence diagonalTestTrajectory =
                robot.driveTrain.trajectorySequenceBuilder(robot.driveTrain.getPoseEstimate())
                        .back(72)
                        .turn(Math.toRadians(180))
                        .back(48)
                        .turn(Math.toRadians(-45))
                        .back(100)
                        .build();
        diagonalTest = trajectorySequenceToStateMachine(diagonalTestTrajectory);
    }

    public void createVisionProvider(int visionProviderIndex) {
        try {
            visionProvider = VisionProviders.VISION_PROVIDERS[visionProviderIndex].newInstance();
        } catch(IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Error while instantiating vision provider");
        }
    }
}
