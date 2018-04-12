package io.github.stefanosbou;

import io.github.stefanosbou.helpers.database.DB;
import io.github.stefanosbou.helpers.database.DBSink;
import io.github.stefanosbou.helpers.deserialization.CustomDeserializationSchema;
import io.github.stefanosbou.kafka.custom.message.CustomMessage;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.github.stefanosbou.helpers.Config.KAFKA_TOPIC;
import static io.github.stefanosbou.helpers.RethinkDbHelper.COUNTRY_STATS;
import static io.github.stefanosbou.helpers.RethinkDbHelper.ORDERS_PER_CURRENCY_PAIR;
import static io.github.stefanosbou.helpers.RethinkDbHelper.TOTAL_ORDERS;

public class FlinkAggregator {

   private static DataStreamSource createKafkaStream(StreamExecutionEnvironment env)  {
      Properties kafkaProperties = new Properties();

      kafkaProperties.put("zookeeper.connect", "localhost:2181");
      kafkaProperties.put("bootstrap.servers", "localhost:9092");
      kafkaProperties.put("group.id", "orders-aggregator-" + UUID.randomUUID());
      kafkaProperties.put("auto.offset.reset", "latest");

      return env
         .addSource(new FlinkKafkaConsumer010<>(KAFKA_TOPIC, new CustomDeserializationSchema(), kafkaProperties));
   }

   private static SingleOutputStreamOperator<Tuple2<String, Integer>> calculateTopCountries(DataStream<CustomMessage.Order> stream) {
      return stream
         .map(new MapFunction<CustomMessage.Order, Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> map(CustomMessage.Order order) {
               return Tuple2.of(order.getOriginatingCountry(), 1);
            }
         })
         // Redistribute by country:
         .keyBy(0)
         .timeWindow(Time.of(1, TimeUnit.SECONDS))
         // Sum by second column:
         .sum(1)
         ;
   }

   private static SingleOutputStreamOperator<Tuple2<String, Integer>> calculateTotalOrders(DataStream<CustomMessage.Order> stream) {
      return stream
         .map(new MapFunction<CustomMessage.Order, Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> map(CustomMessage.Order order) {
               return Tuple2.of("order", 1);
            }
         })
         .keyBy(0)
         .timeWindow(Time.of(1, TimeUnit.SECONDS))
         // Sum by second column:
         .sum(1)
         ;
   }

   private static SingleOutputStreamOperator<Tuple2<String, Integer>> calculateOrdersPerCurrencyPair(DataStream<CustomMessage.Order> stream) {
      return stream
         .map(new MapFunction<CustomMessage.Order, Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> map(CustomMessage.Order order) {
               String currencyPair = order.getCurrencyFrom() + "/" + order.getCurrencyTo();
               return Tuple2.of(currencyPair, 1);
            }
         })
         // Redistribute by currency pair:
         .keyBy(0)
         .timeWindow(Time.of(1, TimeUnit.SECONDS))
         // Sum second column:
         .sum(1)
         ;
   }

   public static void main(String[] args) throws Exception {
      DB.initialize();

      StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();
      env.setStreamTimeCharacteristic(TimeCharacteristic.IngestionTime);
      env.enableCheckpointing(1000, CheckpointingMode.EXACTLY_ONCE);
      env.setParallelism(2);

      DataStream<Tuple2<Long, CustomMessage.Order>> kafkaStream = createKafkaStream(env);
      // Read the value (Order) of each message from Kafka and return it
      DataStream<CustomMessage.Order> eventsStream = kafkaStream.map(new MapFunction<Tuple2<Long, CustomMessage.Order>, CustomMessage.Order>(){
         @Override
         public CustomMessage.Order map(Tuple2<Long, CustomMessage.Order> tuple2) {
            return tuple2.f1;
         }
      });

      calculateTopCountries(eventsStream)
         .addSink(new DBSink(COUNTRY_STATS));
      calculateTotalOrders(eventsStream)
         .addSink(new DBSink(TOTAL_ORDERS));
      calculateOrdersPerCurrencyPair(eventsStream)
         .addSink(new DBSink(ORDERS_PER_CURRENCY_PAIR));

      env.execute("Aggregator");
   }

}
