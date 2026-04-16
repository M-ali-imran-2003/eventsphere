package com.example.eventsphere.utils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class LocationUtil {

    private static final GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);

    public static Point createPoint(Double lat, Double lon) {
        if (lat == null || lon == null) return null;
        // JTS uses (X, Y) which is (Longitude, Latitude)
        return factory.createPoint(new Coordinate(lon, lat));
    }

    public static Double getLat(Point point) {
        return (point != null) ? point.getY() : null;
    }

    public static Double getLon(Point point) {
        return (point != null) ? point.getX() : null;
    }
}
