# MiniBank Microservices Architecture

## 🔧 Arquitectura Corregida

Esta es la nueva arquitectura de microservicios que reemplaza el monolito anterior. Cada servicio tiene su propia base de datos, comunicación asíncrona y deployment independiente.

## 🏗️ Servicios

### 1. Account Service (Puerto: 8081)
- **Responsabilidad**: Gestión de cuentas y balances
- **Base de datos**: `minibank_accounts`
- **API**: `/api/v1/accounts/**`

### 2. Payment Service (Puerto: 8082)
- **Responsabilidad**: Procesamiento de pagos
- **Base de datos**: `minibank_payments`
- **API**: `/api/v1/payments/**`
- **Patrones**: Saga Pattern para transacciones distribuidas

### 3. Ledger Service (Puerto: 8083)
- **Responsabilidad**: Registro contable y auditoría
- **Base de datos**: `minibank_ledger`
- **API**: `/api/v1/ledger/**`

### 4. FX Service (Puerto: 8084)
- **Responsabilidad**: Cambio de divisas y tasas
- **Base de datos**: `minibank_fx`
- **API**: `/api/v1/fx/**`

### 5. API Gateway (Puerto: 8080)
- **Responsabilidad**: Enrutamiento, balanceo de carga
- **Framework**: Spring Cloud Gateway

## 📋 Problemas Solucionados

### ❌ Antes (Monolito Disfrazado)
- Una sola aplicación Spring Boot
- Base de datos compartida
- Transacciones ACID entre dominios
- Llamadas in-process entre servicios
- Acoplamiento fuerte

### ✅ Después (Microservicios Verdaderos)
- **Database per Service**: Cada servicio tiene su BD
- **HTTP/Kafka Communication**: Sin llamadas in-process
- **Saga Pattern**: Transacciones distribuidas
- **Independent Deployment**: Cada servicio se despliega solo
- **Bounded Context**: Dominios bien separados

## 🚀 Cómo Ejecutar

### Opción 1: Docker Compose (Recomendado)
```bash
# Levantar toda la arquitectura
docker-compose -f docker-compose.microservices.yml up

# Solo infraestructura
docker-compose -f docker-compose.microservices.yml up postgres kafka redis

# Servicios individuales
docker-compose -f docker-compose.microservices.yml up account-service
```

### Opción 2: Desarrollo Local
```bash
# Terminal 1: Infrastructure
docker-compose up postgres kafka redis

# Terminal 2: Account Service
cd services/account-service
./gradlew bootRun

# Terminal 3: Payment Service
cd services/payment-service
./gradlew bootRun

# Terminal 4: API Gateway
cd services/api-gateway
./gradlew bootRun
```

## 🔄 Comunicación Entre Servicios

### HTTP (Síncrono)
- Payment Service → Account Service
- API Gateway → Todos los servicios

### Kafka (Asíncrono)
- Events: `payment.requested`, `payment.completed`, `payment.failed`
- Saga Pattern para coordinación
- Event Sourcing para auditoría

## 🗄️ Bases de Datos

Cada servicio tiene su propia base de datos:
```
minibank_accounts  - Account Service
minibank_payments  - Payment Service
minibank_ledger    - Ledger Service
minibank_fx        - FX Service
```

## 📊 Monitoreo

- **Prometheus**: `http://localhost:9090`
- **Grafana**: `http://localhost:3001` (admin/admin)
- **Health Checks**: `http://localhost:808X/actuator/health`

## 🔧 Configuración

Variables de entorno importantes:
```bash
SERVICES_ACCOUNT_URL=http://account-service:8081
SERVICES_LEDGER_URL=http://ledger-service:8083
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

## 🧪 Testing

```bash
# Test individual service
cd services/account-service
./gradlew test

# Test all services
./gradlew test

# Integration tests
./gradlew :payment-service:integrationTest
```

## 🔒 Seguridad

- JWT entre servicios
- API Gateway handles authentication
- Service-to-service mTLS (TODO)

## 📈 Escalabilidad

Cada servicio puede escalarse independientemente:
```bash
# Escalar account service
docker-compose -f docker-compose.microservices.yml up --scale account-service=3
```

## 🗂️ Estructura de Archivos

```
services/
├── account-service/         # Gestión de cuentas
├── payment-service/         # Procesamiento pagos
├── ledger-service/          # Contabilidad
├── fx-service/              # Divisas
├── api-gateway/             # Gateway
└── shared-lib/              # Librerías compartidas (eventos)

docker/
├── init-databases.sql       # Setup BD
└── prometheus.yml          # Config monitoring
```

Esta arquitectura sigue las mejores prácticas de microservicios y resuelve todos los problemas identificados en el monolito anterior.