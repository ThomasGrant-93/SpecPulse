package com.specpulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "specpulse")
public class SpecPulseProperties {

    private Scheduler scheduler = new Scheduler();

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public static class Scheduler {
        private boolean enabled = true;
        private long pullIntervalSeconds = 300;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getPullIntervalSeconds() {
            return pullIntervalSeconds;
        }

        public void setPullIntervalSeconds(long pullIntervalSeconds) {
            this.pullIntervalSeconds = pullIntervalSeconds;
        }
    }
}
