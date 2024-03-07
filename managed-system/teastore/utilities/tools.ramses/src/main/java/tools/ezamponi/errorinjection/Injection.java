package tools.ezamponi.errorinjection;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Injection {

    // percentage value of HTTP successful requests
    public static double changeHttpAvailability(double value, int seconds) {
        double[] probability = new double[1];
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.schedule(() -> {
            probability[0] = value;
            System.out.println("PROBABILITA' IMPOSTATA A "+value+" DOPO "+seconds+ " SECONDI");
        }, seconds, TimeUnit.SECONDS);

        scheduler.shutdown();
        return probability[0];
    }

    public static double resetHttpAvailability(int seconds) {
        double[] probability = new double[1];
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.schedule(() -> {
            probability[0] = 1;
            System.out.println("SUCCESS VARIABLE RE-IMPOSTATA A "+1+" DOPO "+seconds+ " SECONDI");
        }, seconds, TimeUnit.SECONDS);

        scheduler.shutdown();
        return probability[0];
    }

    public static double slowAvailabilityRequests(long value, int seconds) {
        final long[] delay = new long[1];
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.schedule(() -> {
            delay[0] = value;
            System.out.println("RITARDO IMPOSTATO A "+value+" DOPO "+seconds+ " SECONDI");
        }, seconds, TimeUnit.SECONDS);

        scheduler.shutdown();
        return delay[0];
    }

    public static double resetAvailabilityRequests(int seconds) {
        final long[] delay = new long[1];
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.schedule(() -> {
            delay[0] = 0;
            System.out.println("RITARDO RE-IMPOSTATO A "+0+" DOPO "+seconds+ " SECONDI");
        }, seconds, TimeUnit.SECONDS);

        scheduler.shutdown();
        return delay[0];
    }

}
