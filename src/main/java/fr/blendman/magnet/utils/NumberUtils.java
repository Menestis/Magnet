package fr.blendman.magnet.utils;

/**
 * @author Ariloxe
 */
public class NumberUtils {

    public static String timeToStringAll(long seconde) {
        long d = seconde / 86400L;
        long h = seconde % 86400L / 3600L;
        long m = seconde % 3600L / 60L;
        long s = seconde % 60L;
        if (s == 0L && m == 0L && h == 0L && d > 0L)
            return d + " jour" + ((d > 1L) ? "s" : "");
        if (d == 0L && s == 0L && m == 0L && h > 0L)
            return h + " heure" + ((h > 1L) ? "s" : "");
        if (h < 1L && m < 1L && d < 1L)
            return s + "s";
        if (h < 1L && d < 1L)
            return m + "m " + s + "s";
        if (d < 1L)
            return h + "h " + m + "m " + s + "s";
        return d + "j " + h + "h " + m + "m " + s + "s";
    }

}
