package org.firstinspires.ftc.teamcode.robots.reachRefactor.vision.providers;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.robots.reachRefactor.vision.Position;
import org.firstinspires.ftc.teamcode.robots.reachRefactor.vision.VisionProvider;
import org.opencv.core.Mat;

import java.util.HashMap;
import java.util.Map;

public class TensorflowProvider implements VisionProvider {

    private static final String TELEMETRY_NAME = "Tensorflow Provider";

    @Override
    public void initializeVision(HardwareMap hardwareMap) {

    }

    @Override
    public void shutdownVision() {

    }

    @Override
    public Position getPosition() {
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public Map<String, Object> getTelemetry(boolean debug) {
        return new HashMap<>();
    }

    @Override
    public String getTelemetryName() {
        return TELEMETRY_NAME;
    }

    @Override
    public boolean canSendDashboardImage() {
        return false;
    }

    @Override
    public Mat getDashboardImage() {
        return null;
    }

    @Override
    public void update() {

    }
}