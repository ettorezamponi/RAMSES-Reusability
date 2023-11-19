package sefa.restclient.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@Service
public class BenchmarksChangerService {
    @Value("${CHANGE_IMPL_INJECTION}")
    private String changeImplStr;
    @Value("${CHANGE_BENCHMARK_START}")
    private Integer changeBenchmarkStart;
    @Value("${CHANGE_THRESHOLD}")
    private String changeMaxThreshold;
    @Value("${CHANGE_THRESHOLD_START}")
    private Integer changeThresholdStart;
    @Value("${MAX_ART_THRESHOLD}")
    private double maxArtThreshold;
    @Value("${CHANGE_THRESHOLD_DURATION}")
    private Integer changeThresholdDuration;
    @Value("${DEFAULT_THRESHOLD}")
    private double defaultThreshold;

    @Autowired
    private AdaptationController adaptationController;

    @PostConstruct
    public void init() {
        boolean changeImpl = changeImplStr.equalsIgnoreCase("Y");
        boolean changeThresh = changeMaxThreshold.equalsIgnoreCase("Y");
        log.info("BenchmarksChangerService initialized with the following values:");
        log.info("Change Implementation? {}", changeImplStr.equalsIgnoreCase("Y") ? "YES" : "NO");
        log.info("changeBenchmarkStart: {}", changeBenchmarkStart);

        if (changeImpl) {
            Timer timer = new Timer();
            TimerTask changeBenchmark = new TimerTask() {
                @Override
                public void run() {
                    log.info("changeBenchmarkStart: injecting availability to 1 for payment-1-service");
                    adaptationController.updateBenchmarks("PAYMENT-PROXY-SERVICE", "payment-proxy-1-service", "Availability", 1.0);
                }
            };
            timer.schedule(changeBenchmark, changeBenchmarkStart * 1000L);
        }

        if (changeThresh) {
            Timer timer = new Timer();
            TimerTask updateThreshold = new TimerTask() {
                @Override
                public void run() {
                    log.info("Updating max_ART threshold for DELIVERY-PROXY-SERVICE at {}", maxArtThreshold);
                    adaptationController.updateMaxThreshold("DELIVERY-PROXY-SERVICE", maxArtThreshold);
                }
            };
            timer.schedule(updateThreshold,1000L * changeThresholdStart);

            if (changeThresholdDuration != 0) {
                Timer timerStop = new Timer();
                TimerTask resetThreshold = new TimerTask() {
                    @Override
                    public void run() {
                        log.info("Resetting max_ART threshold for DELIVERY-PROXY-SERVICE at {}", defaultThreshold);
                        adaptationController.updateMaxThreshold("DELIVERY-PROXY-SERVICE", defaultThreshold);
                    }
                };
                timerStop.schedule(resetThreshold,1000L * (changeThresholdStart+changeThresholdDuration));
            }
        }

    }
}
