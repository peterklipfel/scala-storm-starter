package storm.starter.topology

import backtype.storm.{ Config, LocalCluster, StormSubmitter }
// import backtype.storm.testing.TestWordSpout
import backtype.storm.topology.TopologyBuilder
import backtype.storm.utils.Utils
import com.hmsonline.storm.cassandra._
import com.hmsonline.storm.cassandra.bolt._
import com.hmsonline.storm.cassandra.bolt.mapper._
import storm.starter.amqp._
import storm.starter.spout._

import scala.collection.JavaConversions._

object ExclamationTopology {
  def main(args: Array[String]) {
    import storm.starter.bolt.ExclamationBolt

    val builder: TopologyBuilder = new TopologyBuilder()

    val configKey : String = "cassandra-config"
    val clientConfigMap = Map(StormCassandraConstants.CASSANDRA_HOST->"localhost:9160",
          StormCassandraConstants.CASSANDRA_KEYSPACE->Array("stormks"))
    val clientConfig =  new java.util.HashMap[String,Object](clientConfigMap)

    // builder.setSpout("word", new TestWordSpout(), 10)
    builder.setSpout("rabbitmq", new AMQPSpout("localhost", 5672, "guest", "guest", "/", new ExclusiveQueueWithBinding("stormExchange", "exclaimTopology"), new AMQPScheme()), 10)
    builder.setBolt("exclaim", new ExclamationBolt(), 3).shuffleGrouping("rabbitmq")
    val cassandraBolt = new CassandraBatchingBolt(configKey, new DefaultTupleMapper("stormks", "stormcf", "word"))
    cassandraBolt.setAckStrategy(AckStrategy.ACK_ON_WRITE) 
    builder.setBolt("cassandra", cassandraBolt)

    val config = new Config()
    config.setDebug(true)
    config.put(configKey, clientConfig)

    if (args != null && args.length > 0) {
      config.setNumWorkers(3)
      StormSubmitter.submitTopology(args(0), config, builder.createTopology())
    } else {
      val cluster: LocalCluster = new LocalCluster()
      cluster.submitTopology("ExclamationTopology", config, builder.createTopology())
      // Utils.sleep(5000)
      // cluster.killTopology("ExclamationTopology")
      // cluster.shutdown()
    }
  }
}
