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
...
 ---> where expirationTime is older than system's time, delete

- A user can only have one cart at a time. When a product manipulation occurs within the timer window, all active product timestamps are updated.
- The timer is 10 minutes.
- If the end_timestamp of the user's userId is older than current time, then the purchase was unsuccessful and the cart is deleted.
- baseline amount of images still available is checked against the transactions, then the Cart (or maybe already at the product stage) needs to check against all other products in the cart table if other users already have this image in their cart (limit is 10)
