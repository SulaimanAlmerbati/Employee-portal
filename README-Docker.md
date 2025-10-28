# Employee Portal - Docker Setup

This document explains how to run the Employee Portal application using Docker and Docker Compose with PostgreSQL database.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose (included with Docker Desktop)
- At least 4GB RAM available for containers

## Quick Start

### 1. Production Setup (Full Application)

```bash
# Build and start all services
docker-compose up --build

# Or run in background
docker-compose up -d --build
```

The application will be available at:
- **Application**: http://localhost:8080
- **pgAdmin**: http://localhost:5050 (admin@employeeportal.com / admin123)

### 2. Development Setup (Database Only)

If you want to run the Spring Boot application locally but use Docker for PostgreSQL:

```bash
# Start only the database
docker-compose -f docker-compose.dev.yml up -d

# Update your local application.properties to use:
# spring.datasource.url=jdbc:postgresql://localhost:5433/employee_portal_dev
```

## Services

### PostgreSQL Database
- **Container**: `employee-portal-db`
- **Port**: 5432 (internal), 5432 (external)
- **Database**: `employee_portal`
- **Username**: `postgres`
- **Password**: `password`

### Spring Boot Application
- **Container**: `employee-portal-app`
- **Port**: 8080
- **Profile**: `docker`
- **Health Check**: http://localhost:8080/actuator/health

### pgAdmin (Database Management)
- **Container**: `employee-portal-pgadmin`
- **Port**: 5050
- **Email**: admin@employeeportal.com
- **Password**: admin123

## Configuration

### Environment Variables

You can customize the application by modifying environment variables in `docker-compose.yml`:

```yaml
environment:
  # Database
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/employee_portal
  SPRING_DATASOURCE_USERNAME: postgres
  SPRING_DATASOURCE_PASSWORD: password
  
  # JWT
  JWT_SECRET: your-secret-key-here
  JWT_EXPIRATION: 3600000
  
  # Email (configure with your SMTP settings)
  MAIL_HOST: smtp.gmail.com
  MAIL_USERNAME: your-email@gmail.com
  MAIL_PASSWORD: your-app-password
```

### Email Configuration

To enable email notifications, update the email environment variables in `docker-compose.yml`:

1. Use your email provider's SMTP settings
2. For Gmail, use an App Password instead of your regular password
3. Update `MAIL_HOST`, `MAIL_USERNAME`, and `MAIL_PASSWORD`

## Docker Commands

### Basic Operations

```bash
# Start services
docker-compose up

# Start in background
docker-compose up -d

# Stop services
docker-compose down

# Rebuild and start
docker-compose up --build

# View logs
docker-compose logs

# View logs for specific service
docker-compose logs app
docker-compose logs postgres
```

### Database Operations

```bash
# Access PostgreSQL container
docker exec -it employee-portal-db psql -U postgres -d employee_portal

# Backup database
docker exec employee-portal-db pg_dump -U postgres employee_portal > backup.sql

# Restore database
docker exec -i employee-portal-db psql -U postgres employee_portal < backup.sql
```

### Application Operations

```bash
# Access application container
docker exec -it employee-portal-app bash

# View application logs
docker-compose logs -f app

# Restart only the application
docker-compose restart app
```

## Data Persistence

- **PostgreSQL data**: Stored in Docker volume `postgres_data`
- **pgAdmin settings**: Stored in Docker volume `pgadmin_data`

To reset all data:
```bash
docker-compose down -v
docker volume prune
```

## Troubleshooting

### Common Issues

1. **Port conflicts**: If ports 5432, 8080, or 5050 are in use:
   - Stop other services using these ports
   - Or modify the ports in `docker-compose.yml`

2. **Database connection issues**:
   - Ensure PostgreSQL container is healthy: `docker-compose ps`
   - Check logs: `docker-compose logs postgres`

3. **Application won't start**:
   - Check if database is ready: `docker-compose logs postgres`
   - Verify environment variables in `docker-compose.yml`
   - Check application logs: `docker-compose logs app`

4. **Out of memory**:
   - Increase Docker Desktop memory allocation
   - Reduce `spring.datasource.hikari.maximum-pool-size`

### Health Checks

```bash
# Check all services status
docker-compose ps

# Check application health
curl http://localhost:8080/actuator/health

# Check database connection
docker exec employee-portal-db pg_isready -U postgres
```

## Development Workflow

### Local Development with Docker Database

1. Start only the database:
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. Update your local `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5433/employee_portal_dev
   ```

3. Run Spring Boot application locally:
   ```bash
   ./mvnw spring-boot:run
   ```

### Full Docker Development

1. Make code changes
2. Rebuild and restart:
   ```bash
   docker-compose up --build
   ```

## Production Considerations

Before deploying to production:

1. **Change default passwords** in `docker-compose.yml`
2. **Set strong JWT secret** via environment variables
3. **Configure proper email settings**
4. **Set up SSL/TLS** (consider using a reverse proxy like nginx)
5. **Configure backup strategy** for PostgreSQL data
6. **Set up monitoring** and log aggregation
7. **Use Docker secrets** for sensitive data

## Security Notes

- Default passwords are for development only
- JWT secret should be at least 32 characters
- Use environment variables or Docker secrets for sensitive data
- Consider running containers as non-root users (already configured)
- Regularly update base images for security patches