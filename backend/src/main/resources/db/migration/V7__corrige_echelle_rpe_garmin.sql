WITH feedback AS (
    SELECT id,
           COALESCE(
               jsonb_path_query_first(details_json::jsonb, '$.**.directWorkoutRpe') #>> '{}',
               jsonb_path_query_first(details_json::jsonb, '$.**.direct_workout_rpe') #>> '{}',
               jsonb_path_query_first(details_json::jsonb, '$.**.perceivedExertion') #>> '{}',
               jsonb_path_query_first(details_json::jsonb, '$.**.perceived_exertion') #>> '{}'
           ) AS raw_rpe
    FROM activity
    WHERE details_json IS NOT NULL
)
UPDATE activity AS activity
SET perceived_exertion_rpe = feedback.raw_rpe::double precision / 10.0
FROM feedback
WHERE activity.id = feedback.id
  AND feedback.raw_rpe ~ '^[0-9]+([.][0-9]+)?$'
  AND feedback.raw_rpe::double precision BETWEEN 0 AND 100;
