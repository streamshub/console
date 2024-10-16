package com.github.streamshub.console.api.support.serdes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

import io.apicurio.registry.serde.avro.DefaultAvroDatumProvider;

public class AvroDatumProvider extends DefaultAvroDatumProvider<RecordData> {
    @Override
    public DatumWriter<RecordData> createDatumWriter(RecordData data, Schema schema) {
        GenericDatumWriter<Object> writer = new GenericDatumWriter<>(schema);

        return new DatumWriter<RecordData>() {
            @Override
            public void write(RecordData data, org.apache.avro.io.Encoder out) throws IOException {
                final DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
                final InputStream dataStream = new ByteArrayInputStream(data.data);
                final Decoder jsonDecoder = DecoderFactory.get().jsonDecoder(schema, dataStream);
                final Object datum = reader.read(null, jsonDecoder);
                writer.write(datum, out);

                /*
                 * Replace input data with the re-seralized record so response contains
                 * the data as it was sent to Kafka (but in JSON format). For example,
                 * unknown fields will have been dropped.
                 */
                final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                final Encoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, buffer);
                writer.write(datum, jsonEncoder);
                jsonEncoder.flush();
                data.data = buffer.toByteArray();
            }

            @Override
            public void setSchema(Schema schema) {
                // No-op
            }
        };
    }

    @Override
    public DatumReader<RecordData> createDatumReader(Schema schema) {
        GenericDatumReader<Object> target = new GenericDatumReader<>(schema);

        return new DatumReader<RecordData>() {
            @Override
            public RecordData read(RecordData reuse, Decoder in) throws IOException {
                final Object datum = target.read(reuse, in);
                final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                final DatumWriter<Object> writer = new GenericDatumWriter<>(schema);
                final Encoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, buffer);
                writer.write(datum, jsonEncoder);
                jsonEncoder.flush();

                return new RecordData(buffer.toByteArray(), null);
            }

            @Override
            public void setSchema(Schema schema) {
                // No-op
            }
        };
    }
}
