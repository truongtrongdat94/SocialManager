package com.socialmanager.job;

import com.socialmanager.model.AutoPilotConfig;
import com.socialmanager.service.AutoPilotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AIAutoPilotGeneratorQuartzJob {

    private final AutoPilotService service;

    /**
     * Scheduled job that runs every minute to check for auto pilot configs
     * that need to be executed
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void runAutoPilot() {
        log.debug("🔍 Checking for Auto Pilot configs to run...");

        List<AutoPilotConfig> configs = service.getConfigsToRun();

        if (configs.isEmpty()) {
            log.debug("✅ No Auto Pilot configs to run at this time");
            return;
        }

        log.info("🚀 Found {} Auto Pilot config(s) to execute", configs.size());

        for (AutoPilotConfig config : configs) {
            try {
                log.info("🤖 Processing Auto Pilot config: {} for user: {}", 
                    config.getId(), config.getUser().getUsername());

                // Execute the auto pilot workflow
                service.executeAutoPilot(config);

                // Update next run time
                service.updateNextRun(config);

                log.info("✅ Successfully processed Auto Pilot config: {}", config.getId());

            } catch (Exception e) {
                log.error("❌ Error processing Auto Pilot config {}: {}", 
                    config.getId(), e.getMessage(), e);
                
                // Still update next run time to avoid getting stuck
                try {
                    service.updateNextRun(config);
                } catch (Exception updateError) {
                    log.error("❌ Failed to update next run time for config {}: {}", 
                        config.getId(), updateError.getMessage());
                }
            }
        }

        log.info("🏁 Auto Pilot job completed. Processed {} config(s)", configs.size());
    }
}