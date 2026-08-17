# Fantasy Storefront

A complete **Java console storefront** for managing a fantasy-item inventory, shopping cart, JSON persistence, sorting, and a separate local administration client.

<p>
  <img src="https://img.shields.io/badge/Java-11-007396?style=flat-square&logo=openjdk&logoColor=white" alt="Java 11" />
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven" />
  <img src="https://img.shields.io/badge/Jackson-JSON-555555?style=flat-square" alt="Jackson JSON" />
  <img src="https://img.shields.io/badge/Tests-JUnit%205-25A162?style=flat-square" alt="JUnit 5" />
  <img src="https://img.shields.io/badge/Status-Complete-238636?style=flat-square" alt="Project status: Complete" />
</p>

## Overview

Fantasy Storefront models a small game-item shop containing weapons, armor, and health items. The user-facing console application loads its inventory from JSON, supports purchasing and canceling items, calculates cart totals, sorts inventory views, and persists stock changes back to disk.

A second console application acts as an administration client. While the storefront is running, the admin client can connect to a background service on the local loopback interface to replace the inventory from a JSON payload or request the current inventory as JSON.

## Features

- Fantasy product hierarchy with weapons, armor, and health items
- JSON-backed inventory persistence
- Default inventory fallback when the JSON file cannot be loaded
- Case-insensitive product lookup
- Inventory quantity validation
- Purchase and purchase-cancellation workflows
- Shopping cart with quantity tracking
- `BigDecimal` cart-total calculations
- Inventory sorting by name or price
- Ascending and descending sort order
- Separate administration console
- Background socket-based administration service
- Admin inventory replacement from JSON
- Admin inventory retrieval as JSON
- Loopback-only administration connection
- Custom file-service exception handling
- Extensive JUnit 5 tests across models and services

## Storefront Workflow

The main application provides these actions:

```text
1) List Inventory
2) Purchase Product
3) Cancel Purchase
4) View Cart
5) View Cart Total
6) Re-Initialize Store
7) Sort Inventory
0) Exit
```

Purchasing an item removes stock from the inventory and adds the requested quantity to the cart. Canceling a purchase performs the reverse operation. When an inventory file is active, stock changes are persisted back to `inventory.json`.

## Product Model

The store uses `SalableProduct` as its shared product abstraction with specialized fantasy-item types:

| Product Type | Example |
| --- | --- |
| Weapon | Steel Sword |
| Armor | Chain Mail |
| Health | Mega Health Potion |

Each inventory item stores a name, description, price, and available quantity. The sample `inventory.json` is included so the application can run with a ready-made inventory.

## Administration Service

`StoreFrontApp` starts `AdministrationService` in a background daemon thread on port `5050`.

For safety, the server is explicitly bound to the **local loopback interface**, so it is intended for communication between applications running on the same machine rather than exposing the administration commands to the local network.

The separate `AdminApplication` supports:

```text
U = Update inventory from a JSON file
R = Return the current inventory as JSON
Q = Quit
```

Requests and responses are serialized as JSON using Jackson.

## Architecture

```text
CST-239/
├── src/
│   ├── app/
│   │   ├── StoreFrontApp.java
│   │   └── AdminApplication.java
│   ├── model/
│   │   ├── SalableProduct.java
│   │   ├── Weapon.java
│   │   ├── Armor.java
│   │   ├── Health.java
│   │   ├── InventoryItemData.java
│   │   ├── AdminCommandRequest.java
│   │   └── AdminCommandResponse.java
│   ├── service/
│   │   ├── StoreFront.java
│   │   ├── InventoryManager.java
│   │   ├── ShoppingCart.java
│   │   ├── FileService.java
│   │   ├── FileServiceException.java
│   │   └── AdministrationService.java
│   └── test/
│       └── JUnit test classes
├── inventory.json
├── admin-update.json
├── pom.xml
└── README.md
```

### Application Layer

The two console applications handle user interaction. `StoreFrontApp` is the customer-facing client, while `AdminApplication` sends administration requests to the running storefront.

### Service Layer

`StoreFront` coordinates inventory and cart operations. `InventoryManager` owns product stock, `ShoppingCart` tracks intended purchases, and `FileService` centralizes JSON serialization and persistence.

### Model Layer

The model package contains the product hierarchy plus data-transfer models used for inventory files and administration messages.

## Persistence

The application uses Jackson to serialize inventory data to JSON. If `inventory.json` cannot be loaded when the storefront starts, a built-in default inventory is created and the application attempts to save it for future runs.

An additional `admin-update.json` file is included as a sample payload for the administration client's update command.

## Testing

The repository contains JUnit tests for the product models, inventory manager, shopping cart, storefront coordination, and file service.

Run the test suite with:

```bash
mvn test
```

## Running the Project

### Requirements

- Java 11 or newer
- Apache Maven

Clone the repository:

```bash
git clone https://github.com/IPFizzy/CST-239.git
cd CST-239
```

Compile and run the storefront:

```bash
mvn compile
mvn exec:java -Dexec.mainClass="app.StoreFrontApp"
```

To use the administration client, leave the storefront running and open a second terminal in the repository directory:

```bash
mvn exec:java -Dexec.mainClass="app.AdminApplication"
```

The administration client connects to `127.0.0.1:5050`.

## Build Cleanup

The project uses Maven for external dependencies rather than committing third-party `.jar` files. Generated class files, Javadocs, Maven output, and IDE-specific project files are excluded from version control so the repository stays focused on source code and project data.

## Practice Project Context

This application originated as a larger Java software-development exercise and is preserved as a completed portfolio project. It demonstrates object-oriented modeling, inheritance, collections, sorting, file persistence, JSON serialization, exception handling, testing, concurrency, and local socket communication in one cohesive application.

## Recommended Repository Name

For a public portfolio, **`FantasyStorefront`** is a stronger repository name than `CST-239` because it describes the finished application instead of the course in which the project originated.

## Author

**Keon Bushman**  
Software Development Student & IT Professional  
[GitHub Profile](https://github.com/IPFizzy)
