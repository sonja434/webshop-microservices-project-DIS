# WebShop Microservices

Mikroservisni sistem za upravljanje web prodavnicom, razvijen korišćenjem Spring Cloud-a.

---

## Opis poslovne logike

Sistem predstavlja backend za web shop aplikaciju i sastoji se od sledećih mikroservisa:

### Poslovni mikroservisi

| Mikroservis | Port | Opis |
|---|---|---|
| user-service | 8081 | Registracija, login, JWT autentifikacija |
| product-service | 8082 | Upravljanje katalogom proizvoda |
| order-service | 8083 | Kreiranje i praćenje porudžbina |
| inventory-service | 8084 | Praćenje i upravljanje zalihama |
| payment-service | 8085 | Obrada plaćanja (asinhrono) |
| notification-service | 8086 | Slanje email notifikacija (asinhrono) |

### Infrastrukturni mikroservisi

| Mikroservis | Port | Opis |
|---|---|---|
| eureka-server | 8761 | Service Discovery |
| config-server | 8888 | Centralizovana konfiguracija |
| api-gateway | 8080 | API Gateway sa JWT filterom |

---

## Dijagram sistema

```
Klijent → API Gateway (8080)
              ↓ JWT validacija
    ┌─────────────────────────┐
    │    Eureka Discovery     │
    │       (8761)            │
    └─────────────────────────┘
              ↓ Service lookup
    ┌──────────────────────────────────────────┐
    │           Poslovni servisi               │
    │                                          │
    │  user-service    product-service         │
    │  (8081)          (8082)                  │
    │                     ↓ REST/Feign         │
    │  inventory-service ← order-service       │
    │  (8084)              (8083)              │
    │                        ↓ RabbitMQ async  │
    │  notification ← payment-service          │
    │  (8086)         (8085)                   │
    └──────────────────────────────────────────┘
              ↓
    ┌─────────────────────────────────┐
    │  PostgreSQL (po jedan DB/servis)│
    └─────────────────────────────────┘
              ↓
    ┌─────────────────────────┐
    │  Config Server (8888)   │
    └─────────────────────────┘
              ↓
    ┌──────────────────────────────────────────┐
    │           Monitoring & Logging           │
    │                                          │
    │  Prometheus → Alertmanager → Email       │
    │  (9090)        (9093)                    │
    │       ↓                                  │
    │  Grafana (3000)                          │
    │                                          │
    │  Fluentd → Elasticsearch → Kibana        │
    │  (24224)    (9200)          (5601)        │
    └──────────────────────────────────────────┘
```

### Komunikacija

**Sinhrona (REST/Feign):**
- `order-service` → `product-service`: provera proizvoda i cene
- `order-service` → `inventory-service`: provera i smanjenje zaliha

**Asinhrona (RabbitMQ):**
- `order-service` → `payment-service`: kreiranje plaćanja po kreiranju porudžbine
- `payment-service` → `notification-service`: slanje email potvrde nakon plaćanja

**Circuit Breaker (Resilience4j):**
- `order-service` → `product-service`: zaštita od pada product servisa
- `order-service` → `inventory-service`: zaštita od pada inventory servisa

---

## Pokretanje sistema

### Preduslovi
- Java 21
- Maven 3.9+
- Docker Desktop

### Razvoj (lokalno)

**Korak 1 – Kopirati environment fajl:**
```bash
cp .env.example .env
```

**Korak 2 – Build svih servisa:**
```bash
mvn clean package -DskipTests
```

**Korak 3 – Pokretanje sistema:**
```bash
docker-compose up -d
```

**Korak 4 – Provera da li sve radi:**
```bash
docker-compose ps
```

**Korak 5 – Praćenje logova:**
```bash
docker-compose logs -f
```

**Zaustavljanje sistema:**
```bash
docker-compose down
```

**Zaustavljanje i brisanje podataka:**
```bash
docker-compose down -v
```

### Produkcija (Kubernetes)

**Korak 1 – Build i push Docker image-a:**
```bash
mvn clean package -DskipTests
docker-compose build
docker-compose push
```

**Korak 2 – Deploy na Kubernetes:**
```bash
kubectl apply -f k8s/
```

---

## Pipeline (CI/CD)

GitHub Actions pipeline se nalazi u `.github/workflows/ci-cd.yml`.

### Faze:

**1. Build and Test** (svaki push i pull request):
- Kompajliranje koda
- Pokretanje unit testova
- Pokretanje integracionih testova
- Čuvanje rezultata testova kao artifact

**2. Build Docker Images** (samo `master` grana):
- Maven package (bez testova)
- Build Docker image-a za sve servise
- Push na GitHub Container Registry (ghcr.io)

**3. Deploy to Development** (samo `master` grana):
- Deploy na development okruženje

### Okruženja:

| Okruženje | Grana | Komanda |
|---|---|---|
| Lokalni razvoj | bilo koja | `docker-compose up -d` |
| Development | master | GitHub Actions automatski |
| Produkcija | master (manual) | `kubectl apply -f k8s/` |

### Pokretanje pipeline-a ručno:
Pipeline se automatski pokreće na svaki `git push` na `master` granu.
Rezultati se mogu pratiti na: `https://github.com/sonja434/webshop-microservices-project-DIS/actions`

---

## Monitoring

### Pristup servisima:

| Servis | URL | Kredencijali |
|---|---|---|
| Eureka Dashboard | http://localhost:8761 | - |
| RabbitMQ Management | http://localhost:15672 | guest/guest |
| Prometheus | http://localhost:9090 | - |
| Alertmanager | http://localhost:9093 | - |
| Grafana | http://localhost:3000 | admin/admin |
| Kibana | http://localhost:5601 | - |
| Elasticsearch | http://localhost:9200 | - |
| API Gateway | http://localhost:8080 | - |

### Prometheus metrike:
Svaki mikroservis izlaže metrike na `/actuator/prometheus` endpointu.
Prometheus prikuplja metrike svakih 15 sekundi.

### Alertmanager – alarmi:
Alertmanager šalje email notifikacije kada se dese sledeći eventi:
- **ServiceDown** – servis nije dostupan više od 1 minuta
- **CircuitBreakerOpen** – circuit breaker je otvoren
- **CircuitBreakerHighFailureRate** – stopa grešaka veća od 50%
- **HighMemoryUsage** – upotreba heap memorije veća od 85%
- **HighErrorRate** – visoka stopa HTTP 5xx grešaka
- **SlowResponseTime** – prosečno vreme odgovora duže od 2 sekunde

### Grafana dashboards:
Nakon pokretanja sistema, Grafana je dostupna na http://localhost:3000.
Dodati Prometheus kao data source: `http://prometheus:9090`
Uvesti dashboard sa ID: `6756` za Spring Boot metrike.

### EFK – centralizovano logovanje:
Svi mikroservisi šalju logove u Fluentd koji ih prosleđuje u Elasticsearch.
Logovi se mogu pregledati u Kibani na http://localhost:5601.

**Podešavanje Kibane:**
1. Otvoriti http://localhost:5601
2. Ići na **Management** → **Index Patterns**
3. Kreirati index pattern: `webshop-*`
4. Izabrati `@timestamp` kao Time Field
5. Pregledati logove u **Discover**

---

## Testiranje

**Pokretanje svih testova:**
```bash
mvn test
```

**Pokretanje testova za određeni servis:**
```bash
mvn test -pl user-service
mvn test -pl product-service
mvn test -pl order-service
mvn test -pl inventory-service
mvn test -pl payment-service
mvn test -pl notification-service
```

**Generisanje test izveštaja:**
```bash
mvn surefire-report:report
```

---

## API Endpointi

### User Service (8081)
| Metoda | Endpoint | Opis | Auth |
|---|---|---|---|
| POST | /api/users/auth/register | Registracija | Ne |
| POST | /api/users/auth/login | Login | Ne |
| GET | /api/users/{id} | Dohvatanje korisnika | Da |
| GET | /api/users | Svi korisnici | Da |

### Product Service (8082)
| Metoda | Endpoint | Opis | Auth |
|---|---|---|---|
| GET | /api/products | Svi proizvodi | Ne |
| GET | /api/products/{id} | Jedan proizvod | Ne |
| POST | /api/products | Kreiranje proizvoda | Da |
| PUT | /api/products/{id} | Izmena proizvoda | Da |
| DELETE | /api/products/{id} | Brisanje proizvoda | Da |

### Order Service (8083)
| Metoda | Endpoint | Opis | Auth |
|---|---|---|---|
| POST | /api/orders | Kreiranje porudžbine | Da |
| GET | /api/orders | Moje porudžbine | Da |
| GET | /api/orders/{id} | Jedna porudžbina | Da |
| PUT | /api/orders/{id}/status | Izmena statusa | Da |

### Inventory Service (8084)
| Metoda | Endpoint | Opis | Auth |
|---|---|---|---|
| POST | /api/inventory | Dodavanje zaliha | Da |
| GET | /api/inventory/{productId} | Stanje zaliha | Da |
| PUT | /api/inventory/{productId} | Izmena zaliha | Da |

### Payment Service (8085)
| Metoda | Endpoint | Opis | Auth |
|---|---|---|---|
| GET | /api/payments/order/{orderId} | Plaćanje po porudžbini | Da |
| GET | /api/payments/user | Moja plaćanja | Da |

---

## Tehnologije

- **Java 21**
- **Spring Boot 3.2**
- **Spring Cloud 2023** (Eureka, Config, Gateway, OpenFeign)
- **RabbitMQ 3.12** – asinhrona komunikacija
- **PostgreSQL 15** – baza podataka (po jedan DB per servis)
- **Resilience4j** – Circuit Breaker
- **Docker & Docker Compose**
- **Prometheus & Grafana** – monitoring performansi
- **Alertmanager** – email alarmi i notifikacije
- **Elasticsearch + Fluentd + Kibana (EFK)** – centralizovano logovanje
- **Kubernetes** – klasterizacija i orkestracija
- **GitHub Actions** – CI/CD pipeline
