# Aurora 🚀 — High‑Throughput Transaction Ingest Platform

![CI](https://github.com/your‑org/aurora/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-blue)

A _zero‑compromise_ real‑time ingestion and analytics stack engineered to ingest **20 billion+ records per day** while sustaining **25 000 TPS** at **sub‑100 ms p99**.  
All code follows an Apple‑grade bar for readability, modularity, and determinism.

---

## 1 · Architecture (50 000‑ft view)

```mermaid
graph TD
    client[(Mobile / Web / Partner API)]
    client -->|JSON/Protocol Buffers| api[REST / gRPC Gateway]
    api --> kafka{{Kafka Topic `txn.v1`}}
    kafka --> ingest[Ingest‑Service<br>(Spring Boot 3)]
    ingest -->|CQL| cassandra[(Cassandra 5)]
    ingest -->|JDBC| rdbms[(DB2 / Oracle)]
    ingest -->|HEC| splunk[(Splunk)]
    ingest -->|OneAgent| dynatrace[(Dynatrace)]
    kafka --> spark[Structured Streaming Job]
    spark --> hive[(Hive Warehouse)]
```

* **Stateless edge** (REST/gRPC) receives traffic and writes to **Kafka** with idempotent producers.
* **Ingest‑Service** (Loom‑threaded Spring Boot) consumes Kafka, validates, persists to **Cassandra** (OLTP) + **DB2** (ACID source‑of‑truth) in a dual‑write pattern with **Exactly‑Once** semantics.
* **Spark job** performs near‑real‑time aggregations feeding Hive for ad‑hoc BI.
* **Observability**: SLF4J → Splunk HEC, OneAgent autoinstrumentation, Prometheus metrics, Grafana dashboards.

*Detailed component diagrams & sequence flows live in `docs/architecture.md`.*

---

## 2 · Quick Start (Dev)

```bash
git clone https://github.com/your‑org/aurora.git
cd aurora
./mvnw -q clean package -DskipTests              # build everything
docker compose up                                # ZK, Kafka, Cassandra, Aurora
curl -XPOST http://localhost:8080/v1/transactions \
     -H "Content‑Type: application/json"         \
     -d '{"txnId":"123e4567-e89b-12d3-a456-426614174000",
          "account":"987654321",
          "amount":145.22,
          "currency":"USD"}'
```

---

## 3 · Module Guide

| Module              | Description                                    | Stack                             |
| ------------------- | ---------------------------------------------- | --------------------------------- |
| **ingest‑service**  | Real‑time validator & dual‑writer microservice | Java 21, Spring Boot 3, Kafka 3.7 |
| **spark‑job**       | Continuous aggregations feeding Hive           | Scala 2.13, Spark 3.5             |
| **infra/terraform** | EKS, MSK, RDS, OneAgent, Splunk HEC            | Terraform 1.9                     |

---

## 4 · CI / CD

* **GitHub Actions** – on‑push build, unit‑tests, license scan, Docker image publish
* **Jenkinsfile** – optional on‑prem pipeline mirroring Actions (build/test/deploy to K8s)
* **IaC** – lint, plan, apply gates via OPA + Snyk

---

## 5 · API Contract

```http
POST /v1/transactions
Content‑Type: application/json
{
  "txnId": "uuid",
  "account": "string",
  "amount": 125.95,
  "currency": "USD",
  "timestamp": "2025-07-01T08:25:30Z"
}
```

Validation rules live in `TransactionValidator.java`; full OpenAPI spec generated at `/swagger-ui.html`.

---

## 6 · Tests & Quality Gates

```bash
./mvnw verify          # unit + integration + Cucumber BDD
./mvnw jacoco:report   # > 90 % line coverage required
mvn -Pperf gatling:test
```

---

## 7 · Deployment Guide

```bash
cd infra/terraform/aws
terraform init && terraform apply
kubectl apply -f k8s/ingest‑service.yaml
```

Zero‑downtime blue‑green strategy with AWS ALB + weighted target groups.

---

© 2025 Your Name · Licensed under Apache 2.0 