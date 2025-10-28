# Employee Portal

A comprehensive web application for employee management, leave requests, payroll access, and company announcements.

## Features

- **User Authentication**: Secure JWT-based authentication with role-based access control
- **Profile Management**: Employee profile viewing and editing
- **Leave Management**: Leave request submission, approval workflow, and history tracking
- **Payroll System**: Payslip viewing and PDF download functionality
- **Announcements**: Company-wide communication system
- **Admin Dashboard**: User management and system metrics

## Technology Stack

- **Backend**: Spring Boot 3.2, Spring Security, Spring Data JPA
- **Database**: PostgreSQL (production), H2 (development/testing)
- **Frontend**: HTML5, CSS3, JavaScript, Bootstrap 5, Thymeleaf
- **Authentication**: JWT tokens
- **Build Tool**: Maven
- **Java Version**: 17

## Project Structure

```
src/
├── main/
│   ├── java/com/company/employeeportal/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Data transfer objects
│   │   ├── exception/      # Custom exceptions
│   │   ├── model/          # JPA entities
│   │   ├── repository/     # Data repositories
│   │   ├── security/       # Security components
│   │   ├── service/        # Business logic
│   │   └── util/           # Utility classes
│   └── resources/
│       ├── static/         # CSS, JS, images
│       ├── templates/      # Thymeleaf templates
│       └── application.properties
└── test/
    └── java/com/company/employeeportal/
        └── [test classes]
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (for production)

### Development Setup

1. Clone the repository
2. Configure database connection in `application.properties`
3. Run with development profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
4. Access the application at `http://localhost:8080`

### Database Setup

For development, the application uses H2 in-memory database by default.

For production, configure PostgreSQL:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/employee_portal
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Running Tests

```bash
mvn test
```

## Configuration

### Environment Profiles

- **dev**: Development environment with H2 database
- **prod**: Production environment with PostgreSQL
- **test**: Test environment for unit/integration tests

### JWT Configuration

Configure JWT settings in `application.properties`:
```properties
app.jwt.secret=your-secret-key
app.jwt.expiration=3600000
app.jwt.refresh-expiration=86400000
```

## API Documentation

The application provides RESTful APIs for:

- Authentication: `/api/auth/**`
- User Management: `/api/users/**`
- Leave Management: `/api/leaves/**`
- Payroll: `/api/payroll/**`
- Announcements: `/api/announcements/**`
- Admin: `/api/admin/**`

## Security

- JWT-based stateless authentication
- Role-based access control (EMPLOYEE, MANAGER, ADMIN)
- BCrypt password encryption
- CORS configuration for frontend integration
- Input validation and sanitization

## Deployment

The application is containerized and ready for deployment on:
- AWS Elastic Beanstalk
- Docker containers
- Traditional application servers

## Contributing

1. Follow the established package structure
2. Write unit tests for new functionality
3. Follow Spring Boot best practices
4. Update documentation as needed

## License

This project is proprietary software for internal company use.