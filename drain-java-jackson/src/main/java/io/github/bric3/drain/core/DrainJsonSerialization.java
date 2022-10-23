/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.drain.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Simple and Drain state serialization mechanism.
 *
 * <strong>DISCLAIMER</strong> This tool allows to (de)serialize the Drain state.
 * But it requires to be in the same package as the Drain class.
 * This is not good solution so be advised that the qualified name of this API
 * might to change.
 *
 * Example use:
 * <pre><code>
 *     Drain drain = ...
 *     Writer writer = ...
 *     DrainJsonSerialization serde = new DrainJsonSerialization();
 *
 *     serde.saveState(drain, writer);
 * </code></pre>
 *
 * <pre><code>
 *     Reader reader = ...
 *     DrainJsonSerialization serde = new DrainJsonSerialization();
 *
 *     Drain drain = serde.loadState(reader);
 * </code></pre>
 *
 * @author brice.dutheil@gmail.com
 */
public class DrainJsonSerialization {

    public static final JsonMapper JSON_MAPPER =
            JsonMapper.builder()
                      .addModule(new SimpleModule()
                                         .addSerializer(Drain.class, new DrainSerializer())
                                         .addDeserializer(Drain.class, new DrainDeserializer())
                                         .addSerializer(Node.class, new TreeNodeSerializer())
                                         .addDeserializer(Node.class, new TreeNodeDeserializer()))
                      .visibility(PropertyAccessor.FIELD, Visibility.ANY)
                      .addMixIn(InternalLogCluster.class, LogClusterMixin.class)
                      .build();

    /**
     * Drain-object exporting functionality which saves a drain model
     * in a json file at given path.
     */
    public void saveState(Drain drain, Writer writer) {
        try {
            JSON_MAPPER.writerWithDefaultPrettyPrinter()
                       .writeValue(writer, drain);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Drain loadState(Reader reader) {
        try {
            return JSON_MAPPER.reader()
                              .withAttribute(ClustersRef.class, new ClustersRef())
                              .readValue(reader, Drain.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class TreeNodeSerializer extends JsonSerializer<Node> {
        @Override
        public void serialize(Node value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeNumberField("depth", value.depth);
            gen.writeObjectField("key", value.key);
            gen.writeObjectField("children", value.childMappings());
            gen.writeArrayFieldStart("clusters");

            for (InternalLogCluster c : value.clusters()) {
                gen.writeString(ClustersRef.toRef(c.clusterId()));
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }

        @Override
        public Class<Node> handledType() {
            return Node.class;
        }
    }

    private static class DrainSerializer extends JsonSerializer<Drain> {
        @Override
        public void serialize(Drain value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeNumberField("effective-depth", value.depth);
            gen.writeNumberField("similarity-threshold", value.similarityThreshold);
            gen.writeNumberField("max-child-per-node", value.maxChildPerNode);
            gen.writeStringField("delimiters", value.delimiters);
            gen.writeObjectField("clusters", value.clusters());
            gen.writeObjectField("prefix-tree", value.prefixTree());
            gen.writeEndObject();
        }

        @Override
        public Class<Drain> handledType() {
            return Drain.class;
        }
    }

    private static class DrainDeserializer extends JsonDeserializer<Drain> {
        @Override
        public Drain deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectCodec codec = p.getCodec();
            JsonNode jsonNode = codec.readTree(p);

            final ArrayList<InternalLogCluster> clusters = codec.readValue(
                    codec.treeAsTokens(jsonNode.get("clusters")),
                    ctxt.getTypeFactory().constructCollectionType(ArrayList.class, InternalLogCluster.class));

            ((ClustersRef) ctxt.getAttribute(ClustersRef.class)).hold(clusters);

            return new Drain(new DrainState(
                    jsonNode.get("effective-depth").asInt(),
                    jsonNode.get("similarity-threshold").asDouble(),
                    jsonNode.get("max-child-per-node").asInt(),
                    jsonNode.get("delimiters").asText(),
                    clusters,
                    ctxt.readValue(codec.treeAsTokens(jsonNode.get("prefix-tree")), Node.class)
            ));
        }

        @Override
        public Class<Drain> handledType() {
            return Drain.class;
        }
    }

    private static class TreeNodeDeserializer extends JsonDeserializer<Node> {
        @Override
        public Node deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final ClustersRef clustersRef = (ClustersRef) ctxt.getAttribute(ClustersRef.class);
            final ObjectCodec codec = p.getCodec();
            final JsonNode jsonNode = codec.readTree(p);

            final ArrayList<InternalLogCluster> nodeClusters = new ArrayList<>();
            for (JsonNode clusterId : jsonNode.get("clusters")) {
                nodeClusters.add(clustersRef.get(clusterId.textValue()));
            }

            final int depth = jsonNode.get("depth").asInt();

            final JsonParser jsonParser = codec.treeAsTokens(jsonNode.get("children"));
            // This json parser starts with JsonTokenId.ID_NO_TOKEN,
            // the map deserializer expects the parser to have already
            // advanced to the first token otherwise this fails with
            // "Unexpected end-of-input ...", to avoid that this parser
            // is advanced to the first token
            jsonParser.nextToken();
            final HashMap<Object, Node> children = ctxt.readValue(
                    jsonParser,
                    ctxt.getTypeFactory()
                        .constructMapType(HashMap.class,
                                          depth == 0 ? int.class : String.class,
                                          Node.class)
            );

            Object key = depth == 1 ?
                         Integer.valueOf(jsonNode.get("key").asInt()) :
                         jsonNode.get("key").asText();

            return new Node(
                    key,
                    depth,
                    children,
                    nodeClusters
            );
        }
    }


    static class ClustersRef {
        private List<InternalLogCluster> clusters;
        private Map<String, InternalLogCluster> clusterIndex;

        public static String toRef(UUID clusterId) {
            return "clusterId-" + clusterId;
        }

        public void hold(List<InternalLogCluster> clusters) {
            this.clusters = clusters;
            this.clusterIndex = clusters.stream().collect(Collectors.toMap(
                    logCluster -> toRef(logCluster.clusterId()),
                    Function.identity()
            ));
        }

        public InternalLogCluster get(String clusterId) {
            final InternalLogCluster logCluster = clusterIndex.get(clusterId);
            assert logCluster != null : "id:" + clusterId + " size:" + clusters.size() + "\n" + clusters;
            return logCluster;
        }
    }

    private static class LogClusterMixin {
        public LogClusterMixin(
                @JsonProperty("clusterId") UUID clusterId,
                @JsonProperty("sightings") int sightings,
                @JsonProperty("logTemplateTokens") List<String> logTemplateTokens) {
        }
    }
}
