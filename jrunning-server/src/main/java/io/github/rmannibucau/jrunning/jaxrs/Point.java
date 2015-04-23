package io.github.rmannibucau.jrunning.jaxrs;

import lombok.Data;

@Data
public class Point {
    private double altitude;
    private double latitude;
    private double longitude;
    private float speed;
    private long timestamp;
}
