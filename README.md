
# Hacken Application

This project is a Spring Boot application for managing and processing blockchain transactions. The application connects to a Web3 provider, processes blocks, and stores transaction data in an H2 database.

## Requirements

- Java 17 or higher
- Maven
- Spring Boot
- Web3j

## Getting Started

Follow the steps below to start the project from the console:

### 1. **Clone the repository**


```bash
git clone https://github.com/RomanPilyushin/Hacken.git
cd Hacken
````

### 2. **Build the project**


```bash
mvn clean install
````


### 3. **Run the application**

To start the application, run the following command from the console:


```bash
mvn spring-boot:run
````


### 4. **Access the application**

Once the application is running, you can access the following functionality via a web browser:

#### H2 Database Console

The H2 Console is available for accessing the in-memory database (H2).

- URL: [http://localhost:8080/h2-console/](http://localhost:8080/h2-console/)
- **JDBC URL**: `jdbc:h2:./data/transactions`
- **Username**: `sa`
- **Password**: (leave it empty by default)

This console allows you to explore the contents of the database in a simple UI.

#### Swagger UI

Swagger UI provides a user-friendly interface for exploring the API endpoints and their documentation.

- URL: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

#### Health Check

You can check the health status of the application using the Spring Actuator's health endpoint.

- URL: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

This endpoint provides information on whether the application is up and running.

#### Metrics

The metrics endpoint provides detailed performance metrics of the application.

- URL: [http://localhost:8080/actuator/metrics](http://localhost:8080/actuator/metrics)

This is useful for monitoring key metrics such as memory usage, HTTP requests, and more.

### **5. Custom Configuration**

**Database**: By default, the application uses an H2 in-memory database. You can configure it to use another database by updating the `application.properties` file.

**Port Configuration**: The application runs on port 8080 by default. You can change this by adding the following property in `application.properties`:

`server.port=8080`