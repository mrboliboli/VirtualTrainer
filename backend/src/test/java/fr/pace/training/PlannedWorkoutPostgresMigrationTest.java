package fr.pace.training;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class PlannedWorkoutPostgresMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("pace_test").withUsername("pace").withPassword("pace_test");

    @Test
    void appliesWorkoutLifecycleAndCompletionMigrationsOnPostgres() {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load();
        assertThat(flyway.migrate().success).isTrue();

        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name='planned_workout' AND column_name IN " +
                "('version','previous_workout_id','completed_activity_id','completion_matched_at','completion_match_method')",
                Integer.class)).isEqualTo(5);
    }
}
