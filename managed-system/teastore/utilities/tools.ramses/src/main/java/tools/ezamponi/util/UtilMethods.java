package tools.ezamponi.util;

import java.util.Random;

public class UtilMethods {
    public static long randomNumber(double min, double max) {
        // Moltiplica per 1000 per spostare i decimali e poi converte in long
        long minLong = (long) (min * 1000);
        long maxLong = (long) (max * 1000);

        Random random = new Random();
        long number = minLong + random.nextInt((int) (maxLong - minLong + 1));

        System.out.println("Random number (long): " + number);
        return number;
    }
}
