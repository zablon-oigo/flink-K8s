package demo;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;

public class HighValuePurchaseFilter {

    private static final String BROKER_URL = "kafka:9092";

    public static void main(String[] args) throws Exception {

        final StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        final ObjectMapper mapper = new ObjectMapper();

        KafkaSource<String> source =
                KafkaSource.<String>builder()
                        .setBootstrapServers(BROKER_URL)
                        .setTopics("sales-raw")
                        .setGroupId("flink-consumer-group")
                        .setStartingOffsets(OffsetsInitializer.earliest())
                        .setValueOnlyDeserializer(new SimpleStringSchema())
                        .build();

        DataStream<JsonNode> filtered =
                env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
                        .map(mapper::readTree)
                        .filter(node ->
                                node.has("price") &&
                                node.get("price").asDouble() > 50);

        KafkaSink<JsonNode> sink =
                KafkaSink.<JsonNode>builder()
                        .setBootstrapServers(BROKER_URL)
                        .setRecordSerializer(
                                new KafkaRecordSerializationSchema<JsonNode>() {

                                    @Override
                                    public ProducerRecord<byte[], byte[]> serialize(
                                            JsonNode element,
                                            KafkaSinkContext context,
                                            Long timestamp) {

                                        try {
                                            String key =
                                                    element.get("customer_id").asText();

                                            String value =
                                                    mapper.writeValueAsString(element);

                                            return new ProducerRecord<>(
                                                    "high-value-purchases",
                                                    key.getBytes(StandardCharsets.UTF_8),
                                                    value.getBytes(StandardCharsets.UTF_8));

                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                })
                        .build();

        filtered.sinkTo(sink);

        env.execute("High Value Purchase Filter");
    }
}