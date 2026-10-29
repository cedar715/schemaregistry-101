# 00 · The lab, and the one idea this project rests on

## The idea

Schema Registry (SR) keeps **no state of its own**. No database, no files. Every schema, version,
compatibility setting and mode lives in one Kafka topic, `_schemas`. When SR starts, it reads that
topic from the beginning and rebuilds everything in memory.

So: **back up `_schemas` and you have backed up the registry.** Everything in this repo builds on that.

## The lab

```
 your laptop:   localhost:9092..9094          localhost:8081            localhost:8080
                       │                             │                         │
                       ▼                             ▼                         ▼
              ┌──────────────────┐  INTERNAL  ┌─────────────────┐       ┌────────────┐
              │ broker-1, -2, -3 │◄───────────│ schema-registry │◄──────│  kafka-ui  │
              │  data, RF 3      │  :29092    │ (state = topic  │       └────────────┘
              │  incl. _schemas  │            │   _schemas)     │
              └────────▲─────────┘            └─────────────────┘
                       │ metadata
              ┌────────┴──────────────────┐
              │ controller-1, -2, -3      │  KRaft quorum, not reachable from the laptop
              └───────────────────────────┘
```

| Piece | Job |
|---|---|
| `controller-1..3` | KRaft quorum. Hold cluster *metadata* (topics, partitions, who leads what). Three, so one can fail. No client ever talks to them. |
| `broker-1..3` | Hold the data. Every topic is replicated 3× with `min.insync.replicas=2`, like production. |
| `schema-registry` | The REST API on top of `_schemas`. |
| `kafka-ui` | Browse topics, messages and subjects at <http://localhost:8080>. |

Brokers have two listeners: `INTERNAL` (`broker-N:29092`) for containers on the Docker network,
and `EXTERNAL` (`localhost:909N`) for your laptop. A client must be able to reach the address a
broker *advertises*, which is why there are two.

All versions are in [`lab/.env`](../../lab/.env) — Confluent Platform 8.1.0 (Kafka 4.1).

## Start it

```bash
docker compose -f lab/docker-compose.yml up -d --wait     # ~30s, ~2.2 GB of memory
```

## Look around

```bash
curl -s localhost:8081/subjects        # []                          nothing registered yet
curl -s localhost:8081/config          # {"compatibilityLevel":"BACKWARD"}   the default
curl -s localhost:8081/mode            # {"mode":"READWRITE"}

docker exec broker-1 kafka-topics --bootstrap-server localhost:29092 --describe --topic _schemas
```

```
Topic: _schemas  PartitionCount: 1  ReplicationFactor: 3  Configs: min.insync.replicas=1,cleanup.policy=compact
```

Read that line carefully — every part of it matters later:

- **`PartitionCount: 1`** — always exactly one. SR needs a single, totally ordered log; with
  more partitions there would be no single order to replay.
- **`ReplicationFactor: 3`** — SR created it from `SCHEMA_REGISTRY_KAFKASTORE_TOPIC_REPLICATION_FACTOR`.
- **`cleanup.policy=compact`** — Kafka keeps only the *latest* record per key, and eventually drops
  keys whose latest value is `null` (a "tombstone"). Hold on to this; it decides what a backup can
  and cannot contain.
- **`min.insync.replicas=1`** — SR set this *on the topic*, overriding the brokers' default of 2.
  A write is acknowledged once one replica has it. Worth checking in your org's cluster.

## The experiment: SR is stateless

```bash
CT='Content-Type: application/vnd.schemaregistry.v1+json'

# 1. register a schema
curl -s -X POST -H "$CT" localhost:8081/subjects/hello-value/versions -d '{"schema":"{\"type\":\"string\"}"}'
# {"id":1,"version":1,...}

# 2. destroy the SR container completely - no volume, nothing kept - and create a new one
docker compose -f lab/docker-compose.yml rm -sf schema-registry
docker compose -f lab/docker-compose.yml up -d --wait schema-registry

# 3. it's all still there
curl -s localhost:8081/subjects        # ["hello-value"]
```

Where did it come from? Read the topic itself:

```bash
docker exec broker-1 kafka-console-consumer --bootstrap-server localhost:29092 --topic _schemas \
  --from-beginning --property print.key=true --property print.offset=true --timeout-ms 5000
```

```
Offset:0  {"keytype":"NOOP","magic":0}                                         null
Offset:1  {"keytype":"NOOP","magic":0}                                         null
Offset:2  {"keytype":"SCHEMA","subject":"hello-value","version":1,"magic":1}   {"subject":"hello-value","version":1,"id":1,"schemaType":"AVRO","schema":"\"string\"","deleted":false}
Offset:3  {"keytype":"NOOP","magic":0}                                         null
Offset:4  {"keytype":"NOOP","magic":0}                                         null
```

- The **key** says *what* a record is (`SCHEMA`, and later `CONFIG`, `MODE`, `DELETE_SUBJECT`...)
  and which subject/version it belongs to. Compaction keeps the latest record per key.
- The **value** is the registry's state for that key.
- **`NOOP`** records: SR writes them at startup to find where the log ends, so it knows when it
  has caught up. Two more appeared when the new container started.

## Questions to answer before stage 01

1. Stop `schema-registry` and keep producing to an *existing* topic with a client that already
   cached its schema. Does it work? What about a client starting fresh?
2. `_schemas` is compacted. If a subject is hard-deleted, what happens to its records over time —
   and what does that mean for a backup taken a week later?
3. Stop one broker (`docker stop broker-2`) and register another schema. Does it work? Now stop a
   second one. What happens, and which setting from above explains it?

## Stop / reset

```bash
docker compose -f lab/docker-compose.yml stop        # keep data
docker compose -f lab/docker-compose.yml down -v     # wipe everything, start fresh next time
```
