/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2012, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package eu.udig.tools.solarterminator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.util.AffineTransformation;

/**
 * Class to calculate Day/Night -Terminator path for the given Calendar object
 * @author Frank Gasdorf
 *
 */
public class DayNightTerminator {

    private static final double X_MAX = 180;
    private static final double X_MIN = 0;
    private static final double Y_MAX = 90;

    private double declination;
    private double gHA;

    public DayNightTerminator(double declination, double gHA) {
        this.declination = declination;
        this.gHA = gHA;
    }

    public DayNightTerminator(final Calendar calendar, final double std) {
        this(DayNight.computeDeclination(calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR),
                std), DayNight.computeGHA(calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR) + 1900, std));
    }

    public DayNightTerminator(final Calendar calendar) {
        this(calendar, getStd(calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)));
    }

    private static double getStd(int hours, int minutes, int seconds) {
        // compute and write Declination
        // double std = 1.0*(hours-locOffset) + minutes/60.0 + seconds/3600.0;

        return 1.0 * hours + minutes / 60.0 + seconds / 3600.0;
    }

    private Coordinate[] getVertices(double declination, double gHA) {
        int F = (declination > 0) ? 1 : -1;

        List<Coordinate> coordinates = new ArrayList<Coordinate>();

        coordinates.add(new Coordinate(X_MIN, Y_MAX + F * 90 - 2));
        double x = 0;
        // step through range from 0 to 360 degree in 1/10 degree steps
        while (x < 360.0) {
            double y = DayNight.computeLat(x, declination);
            coordinates.add(new Coordinate(x, Y_MAX - y));
            x = x + .1;
        }

        coordinates.add(new Coordinate(X_MAX * 2, Y_MAX + F * 90 - 2));
        coordinates.add(new Coordinate(X_MIN, Y_MAX + F * 90 - 2));

        AffineTransformation translationInstance = AffineTransformation
                .translationInstance(-180, -90);
        CoordinateSequence coordinateSequence = new CoordinateArraySequence(
                coordinates.toArray(new Coordinate[] {}));
        for (int i = 0; i < coordinateSequence.size(); i++) {
            translationInstance.transform(coordinateSequence, i);
        }

        return coordinateSequence.toCoordinateArray();
    }

    public Coordinate[] getPolygon() {
        return getVertices(declination, gHA);
    }
}
