package com.socialmanager.job;

import com.socialmanager.model.AutoPilotConfig;
import com.socialmanager.service.autopilot.AutoPilotService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AIAutoPilotGeneratorQuartzJob {

    private final AutoPilotService service;

    public AIAutoPilotGeneratorQuartzJob(AutoPilotService service) {
        this.service = service;
    }

    @Scheduled(fixedRate = 60000) // mỗi phút
    public void runAutoPilot() {

        List<AutoPilotConfig> configs = service.getConfigsToRun();

        for (AutoPilotConfig config : configs) {
            System.out.println("Running AI for config: " + config.getId());

            service.updateNextRun(config);
        }
    }
}