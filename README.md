
# Hacken Application

This Spring Boot application is designed to manage and process Ethereum (ETH) transactions by interacting with the Ethereum blockchain through a Web3 provider (Infura) and storing transaction data in Postgres database. Below is an explanation of how the program works:

### 1. **Fetching Ethereum Transactions from Infura**

The application connects to the Ethereum network using **Infura**, which is a Web3 provider that offers access to Ethereum nodes without running a full node locally. Through the `Web3j` library, the application makes API requests to Infura to fetch Ethereum blockchain data.

Specifically:

- It **subscribes to Ethereum blocks** using the `replayPastAndFutureBlocksFlowable()` method provided by `Web3j`. This means the application:
    - Retrieves historical blocks starting from a specified block (e.g., the last processed block).
    - Continues to monitor and process **future blocks** as they are mined on the Ethereum network.

For each block:

- The program fetches **transactions** that belong to the block.
- Each transaction includes details such as:
    - The **transaction hash** (a unique identifier of the transaction).
    - The **from address** (the sender's Ethereum address).
    - The **to address** (the recipient's Ethereum address).
    - The **transaction value** (the amount of ETH transferred).

### 2. **Storing Ethereum Transactions in the Postgres Database**

Once transactions are fetched from Infura, the application stores them in an **Postgres database**:

- **Postgres** is a lightweight, in-memory database commonly used in development or small-scale applications.
- Each transaction is mapped to a **`TransactionEntity`** (a Java object) and saved into the database using the `TransactionRepository`.

Steps:

- The program **checks for duplicate transactions** using the `transactionHash` (unique identifier). If the transaction already exists in the database, it is not saved again.
- If the transaction is **new**, it is inserted into the database.
- The transaction data is stored with details such as the **from address**, **to address**, and **transaction value**.

### 3. **Retrieving Data from the Postgres Database**

The application provides a **REST API** that allows users to query and retrieve transaction data from the Postgres database:

- **Get Transaction by Hash**: You can fetch a specific transaction from the database by providing its transaction hash.
    - If the transaction is not already in the database, it will be fetched from Infura (via Web3j), saved in the Postgres database, and returned.
- **Get Transactions by Address**:
    - You can retrieve transactions **from a specific address** (i.e., the sender's address).
    - You can retrieve transactions **to a specific address** (i.e., the recipient's address).
- **Search Transactions**: The application provides a flexible search functionality where you can query transactions by **from address**, **to address**, or both.

### Summary of How the Application Works:

1. **Connection to Ethereum Blockchain**: The application connects to Infura via Web3j to fetch Ethereum blocks and transactions.
2. **Transaction Processing**: For each block, the transactions are extracted, and new transactions are saved to the Postgres database.
3. **Transaction Storage**: Transactions are stored in the Postgres database, where details such as the hash, sender, recipient, and value are saved.
4. **Data Retrieval**: Users can query the application’s REST API to retrieve stored transactions based on various criteria (by hash, from address, to address).

### Example Use Case:

1. **Initial Setup**:

    - The application is started, and it begins monitoring Ethereum blocks via Infura.
2. **Fetching Transactions**:

    - A new block is mined, and the application receives the block's data, including its transactions.
    - Each transaction is processed and saved to the Postgres database, if it’s not already present.
3. **Querying Transactions**:

    - A user makes a request to the API, such as `/api/transactions/from/0x123456...` to retrieve all transactions sent from a specific Ethereum address.
    - The application queries the Postgres database and returns the stored transactions matching the request.
4. **Ensuring Consistency**:

    - If a transaction is requested that isn't yet in the Postgres database (e.g., by transaction hash), the application will fetch it from Infura, save it to the database, and return the transaction data.

This setup allows users to efficiently manage and query Ethereum transaction data using a simple, scalable REST API backed by a local database (Postgres).

### Requirements Breakdown and Current Implementation:

#### 1. **Service to Connect to EVM Node and Get Real-Time Transactions**

- **Requirement**: Connect to an EVM-compatible node (like Ethereum, Arbitrum, BSC, Optimism, etc.) using a service such as Infura to retrieve real-time transactions.
    - **Current Implementation**:
        - The application connects to **Infura** (an Ethereum-compatible node) using **Web3j** to retrieve Ethereum transactions in real time.
        - It processes the transactions and stores them in the Postgres database.
- **Fulfillment**: ✅ This requirement is fully implemented.

#### 2. **Start, Stop, Resume Without Losing Transactions**

- **Requirement**: The service should be able to stop, wait for some time, start again, and resume importing transactions without losing any range of transactions.
    - **Current Implementation**:
        - The last processed block number is saved to the database, and when the service restarts, it resumes fetching blocks starting from the last saved block.
        - A `@PreDestroy` method ensures the service gracefully stops, saves the last processed block, and then resumes on restart.
- **Fulfillment**: ✅ This feature is implemented, ensuring no transactions are lost when the service is restarted.

#### 3. **Efficient Storage and Processing of Large Data Volumes**

- **Requirement**: Efficiently handle large transaction volumes, e.g., ~1 million transactions per day, scaling up to millions of records in the database (30 million per month, 360 million per year).
    - **Current Implementation**:
        - The service uses **batch processing** for saving transactions to reduce database write load.
        - The current database is **Postgres**, which is fine for development but not scalable for production. You would need to configure the service to use **PostgreSQL** or **MySQL** for long-term scalability.
        - **Indexes** and potential **partitioning** on large databases will need to be configured when moving to production-grade databases to maintain performance.
- **Fulfillment**: ✅ (for development with Postgres). For production, a switch to a robust database like PostgreSQL or MySQL would be needed.

#### 4. **Store Transaction Data to Database**

- **Requirement**: Store real-time transaction data in the database.
    - **Current Implementation**: Transactions are stored in the Postgres database with details such as `fromAddress`, `toAddress`, `value`, and `transactionHash`.
- **Fulfillment**: ✅ This is fully implemented.

#### 5. **REST Service with Search Functionality**

- **Requirement**: Implement a REST service that allows searching for stored transaction data.
    - **Current Implementation**:
        - The application provides REST endpoints to:
            - Search transactions by `fromAddress`.
            - Search transactions by `toAddress`.
            - Search transactions by both `fromAddress` and `toAddress`.
        - There’s also an endpoint to fetch a transaction by its hash.
- **Fulfillment**: ✅ This functionality is fully implemented.

#### 6. **Optional: Full-Text Search**

- **Requirement**: Add full-text search support as an optional feature.
    - **Current Implementation**: Full-text search is not implemented.
- **Fulfillment**: ❌ Not implemented. You can consider adding a full-text search library (e.g., Hibernate Search with Elasticsearch) if needed.

#### 7. **Optional: API Documentation**

- **Requirement**: API documentation is optional but suggested.
    - **Current Implementation**: The project uses **Swagger** to document the API endpoints. Swagger UI is available at `/swagger-ui/index.html`.
- **Fulfillment**: ✅ Swagger documentation is implemented.

#### 8. **Optional: Health Check**

- **Requirement**: Implement a health check endpoint for the service.
    - **Current Implementation**: The Spring Boot **actuator** is used, and the `/actuator/health` endpoint provides a health check.
- **Fulfillment**: ✅ Health check is implemented.

#### 9. **Optional: Metrics**

- **Requirement**: Provide metrics for monitoring the service's performance.
    - **Current Implementation**: The `/actuator/metrics` endpoint provides various metrics related to the application, such as memory usage, transaction rates, etc.
- **Fulfillment**: ✅ Metrics are implemented via Spring Boot Actuator.

---

### Summary of Fulfillment:

- **Service Connection**: ✅ The service connects to Infura (an Ethereum node) and processes real-time transactions.
- **Start, Stop, Resume**: ✅ The service can stop and resume without losing transaction data.
- **Efficient Data Storage**: ✅ Efficient batch processing for storing transactions is implemented. For production, database configuration changes (PostgreSQL, MySQL) would be needed.
- **REST Service**: ✅ The REST service provides endpoints for searching stored transactions.
- **Optional Features**:
    - **API Documentation**: ✅ Implemented with Swagger.
    - **Health Check**: ✅ Implemented with Spring Boot Actuator.
    - **Metrics**: ✅ Implemented with Spring Boot Actuator.
    - **Full-Text Search**: ❌ Not implemented (optional).

### What’s Missing or Needs Enhancement:

1. **Full-Text Search** (Optional): If needed, you can add this using Elasticsearch or a similar solution.
2. **Database Scalability for Production**: Switch from Postgres to a production database like PostgreSQL or MySQL to handle millions of transactions efficiently.

The current implementation covers **all key requirements** with additional features such as **API documentation**, **health checks**, and **metrics**. For long-term scalability, switching to a more robust database and optimizing the database structure would be recommended for production-level usage.





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

### 2. **Run the application**


```bash
docker-compose up --build
````


### 3. **Access the application**

- URL: [http://localhost:8080/](http://localhost:8080/)

Once the application is running, you can access the following functionality via a web browser:

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

### 4. **Stop the application**


```bash
docker-compose down
````

### **5. Custom Configuration**

**Database**: By default, the application uses Postgres database. You can configure it to use another database by updating the `application.properties` file.

`postgres:5432`

**Port Configuration**: The application runs on port 8080 by default. You can change this by adding the following property in `application.properties`:

`server.port=8080`

