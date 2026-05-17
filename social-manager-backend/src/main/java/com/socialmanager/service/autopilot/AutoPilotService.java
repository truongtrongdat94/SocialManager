package com.socialmanager.service.autopilot;

import com.socialmanager.model.AutoPilotConfig;
import com.socialmanager.model.AutoPilotStatus;
import com.socialmanager.repository.AutoPilotConfigRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AutoPilotService {

    private final AutoPilotConfigRepository repo;

    public AutoPilotService(AutoPilotConfigRepository repo) {
        this.repo = repo;
    }

    public List<AutoPilotConfig> getConfigsToRun() {
        return repo.findByStatusAndNextRunAtBefore(
                AutoPilotStatus.ACTIVE,
                LocalDateTime.now()
        );
    }

    public void updateNextRun(AutoPilotConfig config) {
        config.setLastRunAt(LocalDateTime.now());
        config.setNextRunAt(LocalDateTime.now().plusHours(config.getFrequencyHours()));
        repo.save(config);
    }
}