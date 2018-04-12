package io.github.stefanosbou.helpers;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.exc.ReqlRuntimeError;
import com.rethinkdb.net.Connection;

public class RethinkDbHelper {
   public static final String DATABASE = "orders_dashboard";
   public static final String COUNTRY_STATS = "country_stats";
   public static final String TOTAL_ORDERS = "total_orders";
   public static final String ORDERS_PER_CURRENCY_PAIR = "orders_per_currency_pairs";

   private static final RethinkDB r = RethinkDB.r;

   public static void createDbIfNotExists(Connection conn, String db) {
      try {
         r.dbCreate(db).run(conn);
      } catch (ReqlRuntimeError error){
         // do nothing already exists
      }
   }

   public static void createTableIfNotExists(Connection conn, String db, String table) {
      try {
         r.db(db).tableCreate(table).run(conn);
      } catch (ReqlRuntimeError error){
         // do nothing already exists
      }
   }

   // prevent instatiation
   private RethinkDbHelper(){}
}
