package io.github.stefanosbou.helpers.database;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import io.github.stefanosbou.helpers.RethinkDbHelper;

import java.util.List;
import java.util.Map;

public class DB {
   private static final RethinkDB r = RethinkDB.r;
   private static Connection conn;

   // prevent instantiation
   private DB(){}

   public static void initialize(){
      conn = r.connection().connect();
      RethinkDbHelper.createDbIfNotExists(conn, RethinkDbHelper.DATABASE);
      RethinkDbHelper.createTableIfNotExists(conn, RethinkDbHelper.DATABASE, RethinkDbHelper.COUNTRY_STATS);
      RethinkDbHelper.createTableIfNotExists(conn, RethinkDbHelper.DATABASE, RethinkDbHelper.TOTAL_ORDERS);
      RethinkDbHelper.createTableIfNotExists(conn, RethinkDbHelper.DATABASE, RethinkDbHelper.ORDERS_PER_CURRENCY_PAIR);
   }

   /**
    * Accepts statistics in format: {measurement1 -> count, measurement2 -> count, ...}
    * @param table Target table
    * @param stats Statistics
    */
   public static void insertStats(String table, Map<String, Integer> stats) {
      List array = r.array();
      stats.forEach((k,v) -> {
         array.add(r.hashMap(k, v));
      });
      r.db(RethinkDbHelper.DATABASE).table(table).insert(
         r.hashMap("type", table).with("stats", array)
      ).optArg("conflict", "replace").run(conn);
   }
}
