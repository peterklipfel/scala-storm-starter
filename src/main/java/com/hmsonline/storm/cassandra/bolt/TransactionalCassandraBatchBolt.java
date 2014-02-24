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
package com.hmsonline.storm.cassandra.bolt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.coordination.BatchOutputCollector;
import backtype.storm.coordination.IBatchBolt;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.transactional.ICommitter;
import backtype.storm.tuple.Tuple;

import com.hmsonline.storm.cassandra.bolt.mapper.TupleMapper;

@SuppressWarnings({ "serial", "rawtypes" })
public class TransactionalCassandraBatchBolt<K, C, V> extends CassandraBatchingBolt<K, C, V> implements IBatchBolt,
        ICommitter {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionalCassandraBatchBolt.class);
    private Object transactionId = null;

    public TransactionalCassandraBatchBolt(String clientConfigKey, TupleMapper<K, C, V> tupleMapper) {
        super(clientConfigKey, tupleMapper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void prepare(Map conf, TopologyContext context, BatchOutputCollector collector, Object id) {
        super.prepare(conf, context);
        this.queue = new LinkedBlockingQueue<Tuple>();
        this.transactionId = id;
        LOG.debug("Preparing cassandra batch [" + transactionId + "]");
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        // By default we don't emit anything.
    }

    @Override
    public void execute(Tuple tuple) {
        LOG.debug("Executing tuple for [" + transactionId + "]");
        queue.add(tuple);
    }

    @Override
    public void finishBatch() {

        List<Tuple> batch = new ArrayList<Tuple>();
        int size = queue.drainTo(batch);
        LOG.debug("Finishing batch for [" + transactionId + "], writing [" + size + "] tuples.");
        try {
            this.writeTuples(batch, tupleMapper);
        } catch (Exception e) {
            LOG.error("Could not write batch to cassandra.", e);
        }
    }

}
