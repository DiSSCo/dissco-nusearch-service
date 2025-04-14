package eu.dissco.nusearch.component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageCompressionComponent implements MessageConverter {

  final MessageConverter simpleConverter = new SimpleMessageConverter();

  private static byte[] deflateMessage(byte[] message) throws IOException {
    final int BUFFER_SIZE = 8192; // 8KB
    try (var rstBao = new ByteArrayOutputStream(BUFFER_SIZE)) {
      try (var zos = new GZIPOutputStream(rstBao, BUFFER_SIZE)) {
        zos.write(message);
        zos.flush();
      }
      return rstBao.toByteArray();
    }
  }

  @Override
  public Message toMessage(final Object messageString, final MessageProperties messageProperties)
      throws MessageConversionException {

    if (!(messageString instanceof String)) {
      throw new MessageConversionException("Invalid message type: " + messageString.getClass());
    }
    final byte[] message = ((String) messageString).getBytes(StandardCharsets.UTF_8);

    final byte[] compressedMessage;
    try {
      compressedMessage = deflateMessage(message);
      log.debug(
          "Compressed Length: " + compressedMessage.length + " vs Message Length: " + message.length
              + " / Ratio: " +
              String.format("%.2f%%", compressedMessage.length * 100f / message.length));
      messageProperties.setContentType("application/json");
      messageProperties.setContentEncoding("gzip");
      return new Message(compressedMessage, messageProperties);
    } catch (IOException e) {
      throw new MessageConversionException("Failed to compress message " + messageString, e);
    }
  }

  @Override
  public Object fromMessage(final Message message) throws MessageConversionException {
    var useGzip = "gzip".equals(message.getMessageProperties().getContentEncoding());
    if (useGzip) {
      try {
        try (var reader = new BufferedReader(new InputStreamReader(
            new GZIPInputStream(new ByteArrayInputStream(message.getBody()))))) {
          return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
      } catch (IOException e) {
        throw new MessageConversionException(
            "Failed to decompress message " + new String(message.getBody()), e);
      }
    } else {
      return simpleConverter.fromMessage(message);
    }
  }
}
