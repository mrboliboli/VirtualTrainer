ALTER TABLE activity_synchronization ADD COLUMN search_start_date DATE;
ALTER TABLE activity_synchronization ADD COLUMN catch_up_complete BOOLEAN;

WITH feedback AS (
    SELECT id,
           jsonb_path_query_first(details_json::jsonb, '$.**.directWorkoutRpe') #>> '{}' AS raw_rpe,
           jsonb_path_query_first(details_json::jsonb, '$.**.directWorkoutFeel') #>> '{}' AS raw_feel
    FROM activity
    WHERE source = 'GARMIN_PERSONNEL'
)
UPDATE activity AS a
SET perceived_exertion_rpe = CASE
        WHEN feedback.raw_rpe ~ '^[0-9]+([.][0-9]+)?$'
             AND feedback.raw_rpe::double precision BETWEEN 0 AND 100
        THEN CASE WHEN feedback.raw_rpe::double precision > 10
                  THEN feedback.raw_rpe::double precision / 10
                  ELSE feedback.raw_rpe::double precision END
        ELSE a.perceived_exertion_rpe END,
    garmin_feeling_score = CASE
        WHEN feedback.raw_feel ~ '^[0-9]+([.][0-9]+)?$'
             AND feedback.raw_feel::double precision BETWEEN 0 AND 100
        THEN feedback.raw_feel::double precision
        ELSE a.garmin_feeling_score END,
    subjective_feedback_source = CASE
        WHEN feedback.raw_rpe ~ '^[0-9]+([.][0-9]+)?$'
          OR feedback.raw_feel ~ '^[0-9]+([.][0-9]+)?$'
        THEN 'GARMIN' ELSE a.subjective_feedback_source END
FROM feedback
WHERE a.id = feedback.id;
