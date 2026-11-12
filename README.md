# DiSSCo Name Usage Searcher

Taxonomic resolution of DiSSCo Digital Specimens against the
[Catalogue of Life](https://www.catalogueoflife.org/).
The matching itself is done by GBIF's [matching-ws](https://github.com/gbif/matching-ws), which runs
as a separate service; this application connects RabbitMQ to matching-ws and maps the results onto
the Digital Specimen.

## Flow

- Consumes Digital Specimen events from a RabbitMQ queue, in batches. We could run multiple batches in parallel as the
  matching-ws is capable of handling this.
- Only specimens with a biological topic discipline are resolved; geological and
  extraterrestrial specimens are passed on unchanged.
- For every taxon identification a query is built from the scientific name (or the accepted name
  usage when the scientific name is empty), authorship, epithets, rank and classification.
  All queries of a batch are sent to the matching-ws batch endpoint (`POST /v2/species/match`, at
  most 1000 queries per call).
- When a match is found, the taxon identification is overwritten with the accepted usage from
  matching-ws: scientific name, HTML label, authorship, rank, taxonomic status, epithets and
  classification. The year of publication is parsed from the authorship with the GBIF name parser.
- The original scientific names are stored in `dwc:verbatimIdentification`, separated by a pipe
  `|` when there are multiple taxon identifications.
- A link to the Catalogue of Life taxon is added as an entity relationship (`hasCOLID`).
- The specimen name and topic discipline are updated from the accepted identification.
- The updated event is published to the next RabbitMQ exchange.

## Error handling

Requests to matching-ws are retried up to 3 times (2 seconds apart) on a non-2xx response or an
I/O error. When that fails, the whole batch fails and RabbitMQ redelivers it; after the delivery
limit of the queue the broker moves the messages to the dead letter queue (configured on the
broker, see the deployment repository).

## matching-ws

GBIF publishes matching-ws images that already contain an index, for example of the COL eXtended
Release:

```
docker run --rm -d --name matching-ws -p 8090:8080 \
  -e JVM_OPTIONS="-Xms1g -Xmx6g -XX:+UseZGC -XX:+ParallelRefProcEnabled -XX:+AlwaysPreTouch -XX:+UnlockExperimentalVMOptions" \
  docker.gbif.org/matching-ws:xcol-amd64-316691-20261103-114436
```

Pin the image tag: the tag determines the COL release (and therefore the taxon keys).
Plan at least 8 GB of memory for the matching-ws container.

## Properties

```
# matching-ws
matching-api.endpoint=# Base URL of matching-ws, e.g. http://localhost:8090 (required)
matching-api.time-out-in-sec=# Read timeout for a matching request. Default 60
matching-api.connect-time-out-in-sec=# Connect timeout. Default 5

# RabbitMQ
spring.rabbitmq.host=# The RabbitMQ host
spring.rabbitmq.username=# The RabbitMQ username
spring.rabbitmq.password=# The RabbitMQ password
rabbitmq.queue-name=# Queue to consume from. Default nu-search-queue
rabbitmq.batch-size=# Number of messages per batch. Default 500
rabbitmq.concurrent-consumers=# Number of consumers. Default 1
rabbitmq.exchange-name=# Exchange to publish the processed events to. Default digital-specimen-exchange
rabbitmq.routing-key-name=# Routing key for the processed events. Default digital-specimen

# Application (used as agent on the entity relationship)
application.name=# Default DiSSCo Name Usage Search Service
application.pid=# Default https://doi.org/10.5281/zenodo.14380476
```

## License

This project is licensed under Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
