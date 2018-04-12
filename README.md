## Synopsis

Real-time dashboard application based on Flink Streaming. We collect JSON data POST'd to `/api/orders` endpoint, serialize them using `protobuf` and push them to Apache Kafka. Next, we consume the events from Apache Kafka using Apache Flink, aggregate them by time window and store them to RethinkDB. Finally, we present aggregated insights to user in a dashboard.

As a first step, we create a RESTful API endpoint where we will accept data in the following JSON format:
```
{ 
   "userId": "134256", 
   "currencyFrom": "EUR", 
   "currencyTo": "USD", 
   "amountSell": 1000, 
   "amountBuy": 747.1, 
   "rate": 0.7471, 
   "timePlaced": "24­JAN­15 10:27:44", 
   "originatingCountry": "FR" 
}
```

**All fields are required, so JSON data with missing or mispelled fields and/or mistyped data will be rejected and return a `400 - Bad Request` response.**

The RESTful API was implemented in Java, using the `vert.x` reactive toolkit. This component does not perform any additional processing on the incoming JSON data. It just serializes the data using `protobuf` and pushes the data to Apache Kafka topic named `orders`, so it can handle a large number of messages per second.

This RESTful API component is also responsible for websocket communications between the HTML dashboard and the RethinkDB. There is the `/eventbus` endpoint where the user client (Web browser) subscribes to the `updates` websocket channel. This component has an observer thread that monitors RethinkDB for changes and pushes new data through the `updates` websocket channel to the client's dashboard.

Finally, the RESTful API has some security. The `/api/orders` endpoint where the user can send the data is protected using a JWT Auth Provider, meaning that a POST request to be accepted by the system must have a valid `Authorization: Bearer <token>` header. A valid token can be obtained by making an empty POST request to `/api/token` endpoint and is valid for 1 hour. For simplicity, the `/api/token` endpoint was not secured and returned a valid token on each request. This endpoint can be furthered secured before replying with the valid `JWT token` by accepting a username & a password and validating this data per user before replying with a valid `JWT token`.  

In the aggregation component, we consume all the events from the Apache Kafka `orders topic and aggregate them by time window using the Apache Flink. The aggregated data are then pushed into RethinkDB database. As an example for this task the data is aggregated in 3 ways:
* Sum the data in order to get the total number of orders
* Group by `originatingCountry` field, in order to present the volume of orders per country.
* Group by currency pair `currencyFrom/currencyTo`, in order to present the volume of orders per currency pairs.

**Apache Flink was selected because it required less memory than Apache Spark and was able to be executed in a micro instance in AWS. The same logic (aggregation and data storage to RethinkDB) was also implemented using Apache Spark, but wasn't able to execute it in AWS micro instance for demo.**

As a final step, we create the presentation dashboard where the user can see the aggregated data. This component is created using HTML, JavaScript and WebSockets to communicate with other components and get the necessary data. As mentioned above, there is the `/eventbus` endpoint where the user client (Web browser) subscribes to the `updates` websocket channel and receives updates when new data is pushed to RethinkDB. The dashboard consists of a global map with a realtime visualisation of messages being processed, a counter of the number of messages being processed and a pie chart of the number of messages per currency pair being processed.

The user dashboard also offers the `/token` path, where the user can get a valid `JWT token` using the GUI.

## Screenshots

![Screenshot](https://github.com/stefanosbou/dashboard-with-flink-streaming/raw/master/screenshot.png)

![Screenshot](https://github.com/stefanosbou/dashboard-with-flink-streaming/raw/master/token-generator.png)

## Live Example

**Real-time dashboard:**
`http://ec2-52-15-82-23.us-east-2.compute.amazonaws.com/`

**Endpoint to get a valid token:**
`http://ec2-52-15-82-23.us-east-2.compute.amazonaws.com/token`

**Endpoint to post the JSON data:**
`http://ec2-52-15-82-23.us-east-2.compute.amazonaws.com/api/orders`

example POST request:
```
curl -H "Content-Type: application/json" -X POST -d 
'{ 
   "userId": "134256", 
   "currencyFrom": "EUR", 
   "currencyTo": "USD", 
   "amountSell": 1000, 
   "amountBuy": 747.1, 
   "rate": 0.7471, 
   "timePlaced": 
   "24­JAN­15 10:27:44", 
   "originatingCountry": "IE" 
}'  
http://ec2-52-15-82-23.us-east-2.compute.amazonaws.com/api/orders 
-H "Authorization: Bearer <token>"
```

## Installation

Both the `api-server` and the `aggregator` components use the `common` component, so first we need to build and install this component.

```console
cd common/

mvn clean install
```

For the `api-server`
```console
cd api-server/

mvn clean package

java -cp target/api-server-1.0-SNAPSHOT.jar io.github.stefanosbou.ApiServer
```

For the `aggregator`
```console
cd aggregator/

mvn clean package

java -cp target/aggregator-1.0-SNAPSHOT.jar io.github.stefanosbou.FlinkAggregator
```

