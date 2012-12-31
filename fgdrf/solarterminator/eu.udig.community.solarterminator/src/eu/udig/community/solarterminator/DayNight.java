package eu.udig.community.solarterminator;

import java.util.Calendar;

/**
 * Utility class to compute several parameters to calculate Day Night Terminator
 * 
 * @author Frank Gasdorf
 * 
 */
public final class DayNight {

    private static final double K = Math.PI / 180.0;
    
    private static final int MAX_DAY_OF_YEAR = Calendar.getInstance()
            .getMaximum(Calendar.DAY_OF_YEAR);

    public static double computeDeclination(int dayOfMonth, int monthOfYear,
            int year, double STD) {

        long N;
        double declination;
        double ecliptic, J2000;

        N = MAX_DAY_OF_YEAR * year + dayOfMonth + 31 * monthOfYear - 46;
        if (monthOfYear < 3)
            N = N + (int) ((year - 1) / 4);
        else
            N = N - (int) (0.4 * monthOfYear + 2.3) + (int) (year / 4.0);

        declination = (N - 693960) / 1461.0;
        declination = (declination - (int) declination) * 1440.02509
                + (int) declination * 0.0307572;
        declination = declination + STD / 24.0 * 0.9856645 + 356.6498973;
        declination = declination + 1.91233
                * Math.sin(0.9999825 * declination * K);
        declination = (declination + Math.sin(1.999965 * declination * K)
                / 50.0 + 282.55462) / 360.0;
        declination = (declination - (int) declination) * 360.0;

        J2000 = (year - 2000) / 100.0;
        ecliptic = 23.43929111
                - (46.8150 + (0.00059 - 0.001813 * J2000) * J2000) * J2000
                / 3600.0;

        declination = Math.sin(declination * K) * Math.sin(K * ecliptic);

        return Math.atan(declination
                / Math.sqrt(1.0 - declination * declination))
                / K + 0.00075;
    }

    public static double computeGHA(int dayOfMonth, int monthOfYear, int year,
            double STD) {

        long N;
        double X, XX, P;

        N = MAX_DAY_OF_YEAR * year + dayOfMonth + 31 * monthOfYear - 46;
        if (monthOfYear < 3)
            N = N + (int) ((year - 1) / 4);
        else
            N = N - (int) (0.4 * monthOfYear + 2.3) + (int) (year / 4.0);

        P = STD / 24.0;
        X = (P + N - 7.22449E5) * 0.98564734 + 279.306;
        X = X * K;
        XX = -104.55 * Math.sin(X) - 429.266 * Math.cos(X) + 595.63
                * Math.sin(2.0 * X) - 2.283 * Math.cos(2.0 * X);
        XX = XX + 4.6 * Math.sin(3.0 * X) + 18.7333 * Math.cos(3.0 * X);
        XX = XX - 13.2 * Math.sin(4.0 * X) - Math.cos(5.0 * X)
                - Math.sin(5.0 * X) / 3.0 + 0.5 * Math.sin(6.0 * X) + 0.231;
        XX = XX / 240.0 + 360.0 * (P + 0.5);
        if (XX > 360)
            XX = XX - 360.0;
        return XX;
    }

    public static double computeHeight(final double dec, final double latitude,
            final double longitude, final double GHA) {

        double sinHeight, height;

        sinHeight = Math.sin(dec * K) * Math.sin(latitude * K)
                + Math.cos(dec * K) * Math.cos(K * latitude)
                * Math.cos(K * (GHA + longitude));
        height = Math.asin(sinHeight);
        height = height / K;
        return height;
    }

    public static double computeAzimut(final double dec, final double latitude,
            final double longitude, final double GHA, double hoehe) {
        double cosAz, Az;

        cosAz = (Math.sin(dec * K) - Math.sin(latitude * K)
                * Math.sin(hoehe * K))
                / (Math.cos(hoehe * K) * Math.cos(K * latitude));
        Az = Math.PI / 2.0 - Math.asin(cosAz);
        Az = Az / K;
        // if (Math.sin(K * (GHA + longitude)) < 0)
        // Az = Az;
        if (Math.sin(K * (GHA + longitude)) > 0)
            Az = 360.0 - Az;
        return Az;
    }

    public static double computeLat(double longitude, double dec) {

        double tan, itan;

        tan = -Math.cos(longitude * K) / Math.tan(dec * K);
        itan = Math.atan(tan);
        itan = itan / K;

        return itan;
    }
}
