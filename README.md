
# Hacken Application

This Spring Boot application is designed to manage and process Ethereum (ETH) transactions by interacting with the Ethereum blockchain through a Web3 provider (Infura) and storing transaction data in an H2 in-memory database. Below is an explanation of how the program works:

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

### 2. **Storing Ethereum Transactions in the H2 Database**

Once transactions are fetched from Infura, the application stores them in an **H2 database**:

- **H2** is a lightweight, in-memory database commonly used in development or small-scale applications.
- Each transaction is mapped to a **`TransactionEntity`** (a Java object) and saved into the database using the `TransactionRepository`.

Steps:

- The program **checks for duplicate transactions** using the `transactionHash` (unique identifier). If the transaction already exists in the database, it is not saved again.
- If the transaction is **new**, it is inserted into the database.
- The transaction data is stored with details such as the **from address**, **to address**, and **transaction value**.

### 3. **Retrieving Data from the H2 Database**

The application provides a **REST API** that allows users to query and retrieve transaction data from the H2 database:

- **Get Transaction by Hash**: You can fetch a specific transaction from the database by providing its transaction hash.
    - If the transaction is not already in the database, it will be fetched from Infura (via Web3j), saved in the H2 database, and returned.
- **Get Transactions by Address**:
    - You can retrieve transactions **from a specific address** (i.e., the sender's address).
    - You can retrieve transactions **to a specific address** (i.e., the recipient's address).
- **Search Transactions**: The application provides a flexible search functionality where you can query transactions by **from address**, **to address**, or both.

### Summary of How the Application Works:

1. **Connection to Ethereum Blockchain**: The application connects to Infura via Web3j to fetch Ethereum blocks and transactions.
2. **Transaction Processing**: For each block, the transactions are extracted, and new transactions are saved to the H2 database.
3. **Transaction Storage**: Transactions are stored in the H2 database, where details such as the hash, sender, recipient, and value are saved.
4. **Data Retrieval**: Users can query the application’s REST API to retrieve stored transactions based on various criteria (by hash, from address, to address).

### Example Use Case:

1. **Initial Setup**:

    - The application is started, and it begins monitoring Ethereum blocks via Infura.
2. **Fetching Transactions**:

    - A new block is mined, and the application receives the block's data, including its transactions.
    - Each transaction is processed and saved to the H2 database, if it’s not already present.
3. **Querying Transactions**:

    - A user makes a request to the API, such as `/api/transactions/from/0x123456...` to retrieve all transactions sent from a specific Ethereum address.
    - The application queries the H2 database and returns the stored transactions matching the request.
4. **Ensuring Consistency**:

    - If a transaction is requested that isn't yet in the H2 database (e.g., by transaction hash), the application will fetch it from Infura, save it to the database, and return the transaction data.

This setup allows users to efficiently manage and query Ethereum transaction data using a simple, scalable REST API backed by a local database (H2).

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