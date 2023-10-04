package it.polimi.ramses.execute.rest;

import it.polimi.ramses.execute.domain.ExecuteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path="/rest")
public class ExecuteRestController {

    @Autowired
    private ExecuteService executeService;

    @GetMapping("/start")
    public String start() {
        (new Thread(() -> executeService.execute())).start();
        return "OK";
    }

}
