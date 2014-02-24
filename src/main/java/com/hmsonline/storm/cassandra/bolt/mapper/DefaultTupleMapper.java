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
package com.hmsonline.storm.cassandra.bolt.mapper;

import java.util.HashMap;
import java.util.Map;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class DefaultTupleMapper implements TupleMapper<String, String, String> {
    private static final long serialVersionUID = 1L;
    private String rowKeyField;
    private String columnFamily;
    private String keyspace;

    /**
     * Construct default mapper.
     * 
     * @param keyspace
     *            keyspace to use.
     * @param columnFamily
     *            column family to write to.
     * @param rowKeyField
     *            tuple field to use as the row key.
     */
    public DefaultTupleMapper(String keyspace, String columnFamily, String rowKeyField) {
        this.rowKeyField = rowKeyField;
        this.columnFamily = columnFamily;
        this.keyspace = keyspace;
    }

    @Override
    public String mapToRowKey(Tuple tuple) {
        return tuple.getValueByField(this.rowKeyField).toString();
    }
    
    @Override
    public String mapToKeyspace(Tuple tuple) {
        return this.keyspace;
    }

    /**
     * Default behavior is to write each value in the tuple as a key:value pair
     * in the Cassandra row.
     * 
     * @param tuple
     * @return
     */
    @Override
    public Map<String, String> mapToColumns(Tuple tuple) {
        Fields fields = tuple.getFields();
        Map<String, String> columns = new HashMap<String, String>();
        for (int i = 0; i < fields.size(); i++) {
            String name = fields.get(i);
            Object value = tuple.getValueByField(name);
            columns.put(name, (value != null ? value.toString() : ""));
        }
        return columns;
    }

    @Override
    public String mapToColumnFamily(Tuple tuple) {
        return this.columnFamily;
    }

    @Override
    public Class<String> getKeyClass() {
        // TODO Auto-generated method stub
        return String.class;
    }

    @Override
    public Class<String> getColumnNameClass() {
        // TODO Auto-generated method stub
        return String.class;
    }

    @Override
    public Class<String> getColumnValueClass() {
        // TODO Auto-generated method stub
        return String.class;
    }
    
    
}
