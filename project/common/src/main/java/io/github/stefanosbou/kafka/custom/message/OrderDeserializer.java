package io.github.stefanosbou.kafka.custom.message;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.kafka.common.serialization.Deserializer;

public class OrderDeserializer extends Adapter implements Deserializer<CustomMessage.Order> {

   @Override
   public CustomMessage.Order deserialize(final String topic, byte[] data) {
      try {
         return CustomMessage.Order.parseFrom(data);
      } catch (final InvalidProtocolBufferException e) {
         throw new RuntimeException("Received unparseable message " + e.getMessage(), e);
      }
   }

}