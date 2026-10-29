# schemaregistry-101

Learn Confluent Schema Registry step by step, and build a tool your org can use to **back it up**
by exporting its storage topic (`_schemas`) as raw bytes.

Every stage is one commit: the code it adds, plus a short note in [`docs/learn`](docs/learn) with
the Schema Registry concepts and lab commands to try.

## Roadmap

| # | Stage | You learn | Status |
|---|---|---|---|
| 00 | [Scaffolding + lab](docs/learn/00-lab.md) | SR keeps all its state in `_schemas` | ✅ |
| 01 | Schema Registry basics | subjects, schema IDs, versions, compatibility, the wire format | |
| 02 | Inside `_schemas` | key types, tombstones, compaction — why a raw backup works | |
| 03 | `describe` command | cluster id, partitions, topic configs, offsets via the Admin API | |
| 04 | Raw read | consuming the topic byte for byte | |
| 05 | Consistent snapshot | pinning the end offset: a backup of one instant | |
| 06 | Backup file | JSON Lines, header + trailer, atomic write, SHA-256 | |
| 07 | Tests | Testcontainers against a real Kafka + SR, in CI | |
| 08 | Ops hardening | SASL/SSL, ACLs, exit codes, retention vs `delete.retention.ms`, release | |

## Quick start

Needs Docker (≈2.5 GB free memory) and JDK 21.

```bash
# the lab: 3 KRaft controllers, 3 brokers, Schema Registry, kafka-ui (Confluent Platform 8.1)
docker compose -f lab/docker-compose.yml up -d --wait
curl -s localhost:8081/subjects                 # []
open http://localhost:8080                      # kafka-ui

# the tool
./gradlew build
java -jar build/libs/sr-backup.jar --help
```

Versions: the lab's are in [`lab/.env`](lab/.env), the tool's dependencies in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Layout

```
lab/                    docker compose lab + pinned versions
docs/learn/             one note per stage
src/main/java/...       the sr-backup CLI (Java 21, picocli)
src/test/java/...
.github/workflows/      CI: build + test, jar uploaded as an artifact
```
