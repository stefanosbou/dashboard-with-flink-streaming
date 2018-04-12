package io.github.stefanosbou.helpers.database;


import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.util.HashMap;
import java.util.Map;
/**
 * Aggregates values into a stats structure, and flushes it to a DB
 */
public class DBSink implements SinkFunction<Tuple2<String, Integer>> {

   private Map<String, Integer> stats = new HashMap<>();
   private String tableName;

   public DBSink(String tableName) {
      this.tableName = tableName;
   }

   @Override
   public void invoke(Tuple2<String, Integer> value) {
      System.out.println(value.f0 + " " + value.f1);
      stats.put(value.f0, value.f1);
      DB.insertStats(tableName, stats);
      stats.clear();
   }
}
