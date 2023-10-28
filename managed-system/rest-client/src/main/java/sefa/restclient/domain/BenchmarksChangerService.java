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

    @Autowired
    private AdaptationController adaptationController;

    @PostConstruct
    public void init() {
        boolean changeImpl = changeImplStr.equalsIgnoreCase("Y");
        log.info("BenchmarksChangerService initialized with the following values:");
        log.info("Change Implementation? {}", changeImplStr.equalsIgnoreCase("Y") ? "YES" : "NO");
        log.info("changeBenchmarkStart: {}", changeBenchmarkStart);

        if (changeImpl) {
            Timer timer = new Timer();
            TimerTask changeBenchmark = new TimerTask() {
                @Override
                public void run() {
                    log.info("changeBenchmarkStart: injecting failure");
                    adaptationController.updateBenchmarks("PAYMENT-PROXY-SERVICE", "payment-proxy-2-service", "Availability", 1.0);
                }
            };
            timer.schedule(changeBenchmark, changeBenchmarkStart * 1000L);
        }
    }
}
