package ramses.dashboard.externalinterfaces;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "MONITOR", url = "${MONITOR_URL}")
public interface MonitorClient {

    @PutMapping("/rest/schedulingPeriod")
    void changeSchedulingPeriod(@RequestParam("period") int period);

    @GetMapping("/rest/startRoutine")
    void startRoutine();

    @GetMapping("/rest/stopRoutine")
    void stopRoutine();

    @GetMapping("/rest/")
    GetInfoResponse getInfo();

    @Data
    class GetInfoResponse {
        private int schedulingPeriod;
        private boolean isRoutineRunning;
    }
}

