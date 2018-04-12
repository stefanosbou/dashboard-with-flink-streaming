package io.github.stefanosbou.helpers.deserialization;

import io.github.stefanosbou.kafka.custom.message.CustomMessage;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.util.serialization.KeyedDeserializationSchema;

import java.io.IOException;

public class CustomDeserializationSchema<T> implements KeyedDeserializationSchema<Tuple2<Long, CustomMessage.Order>> {
   @Override
   public Tuple2<Long, CustomMessage.Order> deserialize(byte[] key, byte[] value, String topic, int i, long l) throws IOException {
      Long k = null;
      if(key != null){
         k = Long.parseLong(key.toString());
      }
      CustomMessage.Order v = CustomMessage.Order.parseFrom(value);
      return Tuple2.of(k, v);
   }

   @Override
   public boolean isEndOfStream(Tuple2<Long, CustomMessage.Order> t) {
      return false;
   }

   @Override
   public TypeInformation<Tuple2<Long, CustomMessage.Order>> getProducedType() {
      return TypeInformation.of(new TypeHint<Tuple2<Long, CustomMessage.Order>>() { });
   }
}
