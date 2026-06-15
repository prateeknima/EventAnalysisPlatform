# Runbook: Kafka Consumer Lag High

## Alert

`EventAnalysisKafkaConsumerLagHigh`

## Meaning

The Kafka consumer group `incident-group` is falling behind.

This means incidents may be accepted by the API but not processed quickly enough by the backend consumer.

## Impact

- Incident status may stay as `RECEIVED` or `PROCESSING`
- PostgreSQL persistence may be delayed
- Elasticsearch indexing may be delayed
- Search results may not include recent incidents

## Check Lag

In Prometheus:

`kafka_consumergroup_lag{consumergroup="incident-group"}`

From Kafka directly:

`docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group incident-group`

Look at the `LAG` column.

Healthy value during normal local traffic:

`0` or close to `0`

## Common Causes

### Consumer App Is Not Running

Check that the Spring Boot app is running in IntelliJ.

Check logs for:

`IncidentEventConsumer`

Fix:

- Start the app
- Confirm logs show incidents being consumed

### Consumer Is Throwing Errors

Check IntelliJ logs for exceptions.

Common examples:

- database errors
- Redis errors
- serialization errors
- conflict handling errors

Fix:

- Resolve the exception
- Confirm messages are consumed successfully
- Check if failed messages were sent to DLT

### Dependency Is Slow Or Down

Check Prometheus:

`up{job="postgres"}`

`up{job="redis"}`

Fix:

- Start or restart the failing dependency
- Confirm exporter target returns `UP`

### Consumer Cannot Keep Up With Traffic

If the app is running and no errors are visible, traffic may be higher than consumer throughput.

Fix options:

- Increase Kafka listener concurrency
- Add more consumer instances
- Optimize database writes
- Tune Postgres/Redis connection pools
- Reduce work done per message

## Resolution Criteria

The issue is resolved when:

- Kafka lag returns below the alert threshold
- Lag is no longer increasing
- Consumer logs show successful processing
- New incidents move to `PROCESSED`

## Follow-Up

- Review whether the lag threshold is correct
- Add dashboard panel for Kafka lag
- Add DLT alert if needed
- Document root cause if users were affected