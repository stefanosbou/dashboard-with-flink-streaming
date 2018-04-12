package io.github.stefanosbou;

import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import io.github.stefanosbou.helpers.RethinkDbHelper;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;

import static com.rethinkdb.RethinkDB.r;

public class DBObserverThread extends Thread {
   private static final String EVENTBUS_ADDRESS = "updates";
   private static EventBus bus;

   DBObserverThread(EventBus bus){
      this.bus = bus;
   }

   public void run() {
      Connection conn = null;

      try {
         conn = r.connection().connect();
         RethinkDbHelper.createDbIfNotExists(conn, RethinkDbHelper.DATABASE);
         RethinkDbHelper.createTableIfNotExists(conn, RethinkDbHelper.DATABASE, RethinkDbHelper.COUNTRY_STATS);
         RethinkDbHelper.createTableIfNotExists(conn, RethinkDbHelper.DATABASE, RethinkDbHelper.TOTAL_ORDERS);
         RethinkDbHelper.createTableIfNotExists(conn, RethinkDbHelper.DATABASE, RethinkDbHelper.ORDERS_PER_CURRENCY_PAIR);

         Cursor<HashMap> cur = r.union(
            r.db(RethinkDbHelper.DATABASE).table(RethinkDbHelper.COUNTRY_STATS).changes(),
            r.db(RethinkDbHelper.DATABASE).table(RethinkDbHelper.TOTAL_ORDERS).changes(),
            r.db(RethinkDbHelper.DATABASE).table(RethinkDbHelper.ORDERS_PER_CURRENCY_PAIR).changes()
         ).run(conn);

         while (cur.hasNext()){
            HashMap next = cur.next();
            System.out.println(next);
            bus.publish(EVENTBUS_ADDRESS, parseChanges(next));
         }
      }
      catch (Exception e) {
         System.err.println("Error: " + e.getLocalizedMessage());
      }
      finally {
         assert conn != null;
         conn.close();
      }
   }

   private JsonObject parseChanges(HashMap changes) {
      return new JsonObject(changes).getJsonObject("new_val");
   }

}
