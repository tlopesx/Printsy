# Data Types & Schemas

#### Product Table --> MySQL

| Field            | Data Type          | Details                           | Example                                  |
|------------------|--------------------|-----------------------------------|------------------------------------------|
| product_id        | ID                 | Primary key desired product combo | 44412345                                 | 
| image_id          | ID                 | Image Table foreign key           | 22212345                                 | 
| stock_id          | ID                 | Stock Table foreign key           | 33312345                                 | 
| price     | Integer              | basePrice + imageSurcharge        | 79.98                                    | ---> aggregation


#### Cart Table --> MySQL

| Field            | Data Type          | Details                              | Example               |
|------------------|--------------------|--------------------------------------|-----------------------|
| user_id           | ID                 | Foreign Key with User table          | 66612345              |
| product_id        | ID                 | Foreign Key with Product table       | 
| expiration_time   | String/Integer     | Timestamp of cart expiration (+10min)| "2023-03-10 02:10:00" |


- A user can only have one cart at a time. When a product manipulation occurs within the timer window, all active product timestamps are updated.
- The timer is 10 minutes.
- If the end_timestamp of the user's userId is older than current time, then the purchase was unsuccessful and the cart is deleted.
- baseline amount of images still available is checked against the transactions, then the Cart (or maybe already at the product stage) needs to check against all other products in the cart table if other users already have this image in their cart (limit is 10)






## README for Cart Service

### Introduction to the Cart Service

The Cart Application is a vital component of Printsy, a larger microservice-based web application designed to offer users the ability to create personalized merchandise. Leveraging OpenAI's image generation service, Printsy allows users to generate their own images or select from a public library of pre-generated artwork, which can then be printed on mugs and t-shirts. To ensure uniqueness and originality, Printsy enforces a 10-print cap across all publicly available images.

### Background on Printsy

Printsy is composed of several microservices, each fulfilling a specific role within the application:

- **Authentication Service**: Manages user account creation and user data storage.
- **Generation Service**: Connects to the OpenAI API for image generation and stores generated images in IBM Cloud.
- **Gallery Service**: Stores image URLs and image IDs, along with metadata such as the number of likes.
- **Cart Service**: Manages the checkout process, ensuring smooth transactions and adherence to the 10-print cap.
- **Transaction Service**: Records payment history and items purchased on the Printsy blockchain, maintaining a historical record of transactions.
- **Frontend React Service**: Connects with each of these services through Printsy's API Gateway, which enhances scalability, fault tolerance, and load balancing of requests.

### About the Cart Application

The Cart Application specifically handles the shopping cart functionalities within Printsy. It allows users to add items to their cart, complete purchases, and manage their cart items efficiently. The application provides a GraphQL API for querying and mutating cart and product data, interfaces with external services for image and transaction management, and includes a robust task management system to handle asynchronous processing of cart items.

This README will guide you through the key components of the Cart Application, including the available GraphQL mutations and queries, details on cart and product entities, the integration package for external services, and an in-depth look at the tasks package.

### GraphQL Mutations and Queries

**Available Queries**
- `getCartItemsByUserId(userId: Long!): [CartResult]`: Fetches all cart items for a specific user.
- `getCartProductsByUserId(userId: Long!): [ProductResult]`: Checks the products in the cart for a specific user.
- `getImageUrlsByImageIds(imageIds: [String]!): [String]`: Retrieves image URLs for a list of image IDs.

**Available Mutations**
- `addItemToCart(imageId: String!, stockId: Long!, price: Int!, userId: Long!): String`: Adds an item to the cart.
- `completePurchase(userId: Long!): Boolean`: Completes the purchase process for a user.
- `deleteCartItemsByUserId(userId: Long!): Boolean`: Deletes all cart items for a user.

---

### Cart and Product Entities

#### Cart Entity
| Property         | Type     | Description                        |
|------------------|----------|------------------------------------|
| cartId           | Long     | Unique identifier for the cart     |
| userId           | Long     | Identifier of the user             |
| product          | Product  | Associated product                 |
| expirationTime   | Instant  | Expiration time of the cart item   |

#### Product Entity
| Property         | Type     | Description                        |
|------------------|----------|------------------------------------|
| productId        | Long     | Unique identifier for the product  |
| imageId          | String   | Identifier for the product image   |
| stockId          | Long     | Stock identifier                   |
| price            | Integer  | Price of the product               |

---

### Integration Package

The integration package contains services that interface with external systems to fetch gallery images and manage transactions.

#### Gallery Service
- **Purpose**: Interacts with the external gallery service to fetch image URLs.
- **Key Methods**:
 - `getImageUrl(imageId: String): String`: Fetches the URL of an image given its ID.
 - `getImageUrlsByImageIds(imageIds: List<String>): Map<String, String>`: Fetches URLs for multiple image IDs.

#### Transaction Gateway Service
- **Purpose**: Manages transaction operations with the external transaction gateway.
- **Key Methods**:
 - `getTransactionImageAvailability(imageId: String): int`: Checks the availability of an image in transactions.
 - `completeTransaction(transactions: List<TransactionInput>): boolean`: Completes transactions for a list of transaction inputs.

---

### Tasks Package

#### Overview
The tasks package handles asynchronous processing of cart items. It ensures tasks are processed in a first-come, first-served manner for each image, while allowing concurrent removals from the queue.

#### Components
- **PendingCartItem**: Represents an object with the necessary info to create a cart item. 
- **CartQueue**: Manages the queue of `PendingCartItem` objects. There is a single queue for each imageId, ensuring that pending cart items for a single image are processed one by one. 
Once a pending cart item is added to the database, a cleanup task is created to manage its deletion at cart expiration.
- **CleanUpService**: Manages the cleanup of expired cart items.
- **CartQueueService**: Handles the management and processing of tasks within the queue.

- **TaskSchedulerService**: Schedules tasks for future execution and manages their lifecycle.
- **QueueManager**: A unified interface for interacting with the task queue.

#### How it Works
1. **Enqueue Operation**: Tasks are added to the queue using the `enqueue` method, which ensures that tasks are added in a thread-safe manner.
2. **Processing Tasks**: The `processAllQueues` method continuously checks for tasks in the queue and processes them using a dedicated executor for each image ID.
3. **Handling Concurrent Operations**: The queue allows concurrent removals while maintaining strict ordering for additions, ensuring that tasks are processed in the order they were added.
4. **Task Scheduling and Cleanup**: The `TaskSchedulerService` manages the scheduling of tasks, while the `CleanUpService` handles the removal of expired items.

---

This README provides an overview of the Cart Application, detailing its GraphQL API, core entities, integration services, and the task management system. For further information or specific details about the GraphQL operations and other components, please refer to the respective sections in the documentation.
