package fr.pace.garmin;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GarminConnectorProperties.class)
public class GarminConnectorConfiguration {
}
