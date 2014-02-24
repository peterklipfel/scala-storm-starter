/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hmsonline.storm.cassandra.trident;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import storm.trident.operation.Filter;
import storm.trident.operation.Function;
import storm.trident.operation.TridentCollector;
import storm.trident.operation.TridentOperationContext;
import storm.trident.tuple.TridentTuple;
import backtype.storm.tuple.Values;

import com.hmsonline.storm.cassandra.bolt.mapper.Equality;
import com.hmsonline.storm.cassandra.bolt.mapper.TridentColumnMapper;
import com.hmsonline.storm.cassandra.bolt.mapper.TridentTupleMapper;
import com.hmsonline.storm.cassandra.client.AstyanaxClient;
import com.hmsonline.storm.cassandra.client.AstyanaxClientFactory;

public class TridentCassandraLookupFunction<K, C, V> implements Function {
    private static final long serialVersionUID = 12132012L;

    private static final Logger LOG = LoggerFactory.getLogger(TridentCassandraLookupFunction.class);

    private TridentColumnMapper<K, C, V> columnsMapper;
    private TridentTupleMapper<K, C, V> tupleMapper;
    private AstyanaxClient<K, C, V> client;
    private String cassandraClusterId;

    private Filter tupleFilter = null; // used to prevent processing for tuples
                                       // that should be skipped by the lookup
    private int numberOfOutputFields = 1; // used to emit when the incoming
                                          // tuple doesn't pass the filter check
    private boolean emitEmptyOnFailure = false;

    /**
     * @param cassandraClusterId Unique identifier for the Cassandra cluster
     * @param tupleMapper
     * @param columnMapper
     */
    public TridentCassandraLookupFunction(String cassandraClusterId, TridentTupleMapper<K, C, V> tupleMapper,
            TridentColumnMapper<K, C, V> columnMapper) {
        this.columnsMapper = columnMapper;
        this.tupleMapper = tupleMapper;
        this.cassandraClusterId = cassandraClusterId;
    }

    /**
     * @param cassandraClusterId Unique identifier for the Cassandra cluster
     * @param tupleMapper
     * @param columnMapper
     * @param emitEmptyOnFailure
     */
    public TridentCassandraLookupFunction(String cassandraClusterId, TridentTupleMapper<K, C, V> tupleMapper,
            TridentColumnMapper<K, C, V> columnMapper, boolean emitEmptyOnFailure) {
        this(cassandraClusterId, tupleMapper, columnMapper);
        this.emitEmptyOnFailure = emitEmptyOnFailure;
    }

    public void setFilter(Filter filter) {
        this.tupleFilter = filter;
    }

    public void setNumberOfOutputFields(int numberOfFields) {
        this.numberOfOutputFields = numberOfFields;
    }

    public void setEmitEmptyOnFailure(boolean emitEmptyOnFailure) {
        this.emitEmptyOnFailure = emitEmptyOnFailure;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void prepare(Map stormConf, TridentOperationContext context) {
        Map<String, Object> config = (Map<String, Object>) stormConf.get(this.cassandraClusterId);
        this.client = AstyanaxClientFactory.getInstance(cassandraClusterId, config);
    }

    @Override
    public void cleanup() {
        this.client.stop();
    }

    @Override
    // TODO come back and fix this once composite range queries are sorted out
    // we should not have to execute multiple queries.
    public void execute(TridentTuple input, TridentCollector collector) {
        if (tupleFilter != null && !tupleFilter.isKeep(input)) {
            collector.emit(createEmptyValues());
            return;
        }

        K rowKey = null;
        try {
            rowKey = tupleMapper.mapToRowKey(input);
            C start = tupleMapper.mapToStartKey(input);
            C end = tupleMapper.mapToEndKey(input);
            List<C> list = tupleMapper.mapToColumnsForLookup(input);

            List<Values> valuesToEmit;
            Map<C, V> colMap = null;
            
            if (list != null){
                colMap = client.lookup(tupleMapper, input, list);
            } else if (start != null && end != null){
                colMap = client.lookup(tupleMapper, input, start, end, Equality.GREATER_THAN_EQUAL);
            } else {
                    colMap = client.lookup(tupleMapper, input);                
            }

            valuesToEmit = columnsMapper.mapToValues(rowKey, colMap, input);
            if(valuesToEmit != null){
                for (Values values : valuesToEmit) {
                    collector.emit(values);
                }
            }

        } catch (Exception e) {
            if (this.emitEmptyOnFailure) {
                LOG.info("Error processing tuple and will be emitting empty values.");
                collector.emit(createEmptyValues());
            }
            LOG.warn("Could not emit for row [" + rowKey + "] from Cassandra." + " :" + e.getMessage(), e);
        }
    }

    private Values createEmptyValues() {
        ArrayList<Object> emptyValues = new ArrayList<Object>();
        for (int evc = 0; evc < this.numberOfOutputFields; evc++) {
            emptyValues.add("");
        }
        return new Values(emptyValues.toArray());
    }
}
