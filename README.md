# Developer Guide for Ledger Application

## Prerequisites

Before you can install and run the Ledger Application, you need to have the following installed on your system:

- Java Development Kit (JDK) 11 or later
- Apache Maven 3.6.0 or later

## Installation

1. **Clone the Repository:** Clone the Ledger Application repository from GitHub to your local machine. You can do this
   by running the following command in your terminal:

```bash
git clone https://github.com/maithilicharan/ledger-application.git
```

2. **Navigate to the Project Directory:** Change your current directory to the Ledger Application directory:

```bash
cd ledger-application
```

3. **Build the Project:** Build the project using Maven. This will compile the code, run tests, and create a JAR file in
   the `target` directory:

```bash
mvn clean install
```

## Known Issues and TODOs

1. **Error Handling**: The current implementation of the Ledger Application does not provide detailed error messages.
   This can make it difficult for clients to understand what went wrong when an operation fails.
2. **Hard coding**: At the moment Ledger application hard codes Account and Wallet Management.
   This needs to be removed and comprehensive Account and Wallet Management needs to be implemented. Apparently this
   needs bit of time to implement
3. **Database** : At the moment application using InMemory database. This needs to be replaced with actual database
   implementation, respective scaffolding and the support from Axon framework already exists.
4. **Final Point** :  Application needs thorough testing around end to end scenarios. This could be easily done, since I
   have
   the framework in place to perform further development and testing.
