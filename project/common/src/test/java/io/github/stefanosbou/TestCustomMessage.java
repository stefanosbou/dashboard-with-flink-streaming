package io.github.stefanosbou;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.github.stefanosbou.kafka.custom.message.CustomMessage;
import org.junit.Before;
import org.junit.Test;

public class TestCustomMessage {

   @Before
   public void setup() throws InvalidProtocolBufferException {
      String json = "";
      CustomMessage.Order.Builder builder = CustomMessage.Order.newBuilder();
      JsonFormat.parser().merge(json, builder);
      CustomMessage.Order order = builder.build();
   }

   @Test
   public void
}
