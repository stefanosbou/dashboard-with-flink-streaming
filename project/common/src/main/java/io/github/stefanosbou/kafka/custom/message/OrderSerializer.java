package io.github.stefanosbou.kafka.custom.message;

import org.apache.kafka.common.serialization.Serializer;

public class OrderSerializer extends Adapter implements Serializer<CustomMessage.Order> {
   @Override
   public byte[] serialize(final String topic, final CustomMessage.Order data) {
      return data.toByteArray();
   }
}
