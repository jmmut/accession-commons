package uk.ac.ebi.eva.benchmarking_suite

import com.datastax.driver.core.{Cluster, ConsistencyLevel, PreparedStatement, SimpleStatement}
import com.mongodb.ReadConcern
import org.mongodb.scala.{Document, MongoClient, MongoCollection, WriteConcern}
import org.apache.jmeter.control.LoopController
import org.apache.jmeter.samplers.{AbstractSampler, Sampler}
import org.apache.jmeter.threads.ThreadGroup
import org.apache.jmeter.util.JMeterUtils
import org.rogach.scallop._
import uk.ac.ebi.eva.benchmarking_suite.cassandra.{CassandraConnectionParams, CassandraReadSampler, CassandraWriteSampler}
import uk.ac.ebi.eva.benchmarking_suite.mongodb.{MongoDBConnectionParams, MongoDBReadSampler, MongoDBWriteSampler}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object BenchmarkingMain extends App {

  //Supported databases
  val validDatabases = List("cassandra", "mongodb")

  //Parse command line arguments
  val config = new BenchmarkingArgParser(args, validDatabases)

  //Stateful JMeter initialization
  initJMeterEnvironment(config.JMeterHome())
  var jmeterProperties = JMeterUtils.getJMeterProperties

  //Database initialization
  val (db, connectionString, schemaName, variantTableName) =
    (config.databaseType(), config.connectionString(), config.schemaName(), config.variantTableName())
  val dbConnectionParams = getDBConnectionParams(db, connectionString)
  jmeterProperties.put("connectionParams", dbConnectionParams)

  val readSamplers = Map("cassandra" -> classOf[CassandraReadSampler], "mongodb" -> classOf[MongoDBReadSampler])
  val writeSamplers = Map("cassandra" -> classOf[CassandraWriteSampler], "mongodb" -> classOf[MongoDBWriteSampler])

  //Description Format - <Type of Operation>-<Operations per Workload Unit (WU)>-<Parallel(par)/Sequential(seq)>
  val writeWorkloads = List(
    new Workload(desc = "ins-256k-par", threadChoices = List(4, 8, 16, 32), numOpsPerWU =  256000, numWU = 50),
    new Workload(desc = "ins-1B-par", threadChoices = List(8, 16, 32), numOpsPerWU = 1e9.toInt, numWU = 2),
    new Workload(desc = "ins-32k-seq", threadChoices = List(1), numOpsPerWU =  32000, numWU = 50)
  )
  val readWorkloads = List(
    new Workload(desc = "read-256k-par", threadChoices = List(4, 8, 16, 32), numOpsPerWU = 256000, numWU = 50),
    new Workload(desc = "read-1B-par", threadChoices = List(8, 16, 32), numOpsPerWU = 1e9.toInt, numWU = 2),
    new Workload(desc = "read-32k-seq", threadChoices = List(1), numOpsPerWU = 32000, numWU = 50)
  )

  try {
    var jMeterTestPlan = new JMeterTestPlan(testOutputFile = config.outputFile())
    //Add a separate thread group + sampler to the Test plan for each thread choice in a given workload
    writeWorkloads.flatMap(workload => getJMeterThreadGroupAndSampler(workload, writeSamplers(db)))
      .foreach(x => jMeterTestPlan.addThreadGroupAndSampler(x._1, x._2))
    readWorkloads.flatMap(workload => getJMeterThreadGroupAndSampler(workload, readSamplers(db)))
      .foreach(x => jMeterTestPlan.addThreadGroupAndSampler(x._1, x._2))
    jMeterTestPlan.runPlan()
  } catch {
    case ex: Exception => ex.printStackTrace()
  }
  finally {
    dbConnectionParams.cleanup()
  }

  def getDBConnectionParams(db: String, connectionString: String): DBConnectionParams = {
    db match {
      case "cassandra" => getCassandraConnectionParams(connectionString, schemaName, variantTableName)
      case "mongodb" => getMongoDBConnectionParams(connectionString, schemaName, variantTableName)
    }
  }

  def getCassandraConnectionParams(connectionString: String, keyspaceName: String, variantTableName: String):
  CassandraConnectionParams = {
    val cassandraNodes = connectionString.split(",")
    val cassandraCluster = Cluster.builder().addContactPoints(cassandraNodes: _*).build()
    val cassandraSession = cassandraCluster.connect(keyspaceName)

    val insertEntityString = "insert into %s (species, chromosome, start_pos, entity_id, accession_id, raw_numeric_id) "
      .format(variantTableName) +
      "values (?, ?, ?, ?, ?, ?);"
    val blockReadString = "select * from %s where species = ? and chromosome = ? and start_pos >= ? and start_pos <= ?"
      .format(variantTableName)
    val preparedInsertStatement: PreparedStatement = cassandraSession.prepare(insertEntityString)
    val blockReadStatement: PreparedStatement = cassandraSession.prepare(blockReadString)
      .setConsistencyLevel(ConsistencyLevel.QUORUM)

    cassandraSession.execute(new SimpleStatement("truncate %s".format(variantTableName)).setReadTimeoutMillis(600000))

    CassandraConnectionParams(cassandraCluster, cassandraSession, preparedInsertStatement, blockReadStatement)
  }

  def getMongoDBConnectionParams(connectionString: String, databaseName: String, collectionName: String):
  MongoDBConnectionParams = {
    val mongoClient = MongoClient(connectionString)
    val mongoDatabase = mongoClient.getDatabase(databaseName)
    var mongoCollection: MongoCollection[Document] = mongoDatabase.getCollection(collectionName)
      .withWriteConcern(WriteConcern.MAJORITY)

    Await.result(mongoCollection.deleteMany(Document()).toFuture(), Duration.Inf)

    MongoDBConnectionParams(mongoClient, mongoDatabase, mongoCollection)
  }

  def getJMeterThreadGroupAndSampler(workload: Workload, sampler: Class[_<: AbstractSampler]):
  List[(ThreadGroup, Sampler)] = {
    workload.threadChoices.map(numThreads =>
      getThreadGroupAndSamplerForWorkload(
        name = "%s-%d-threads".format(workload.desc, numThreads),
        numWU = workload.numWU,
        numThreads = numThreads,
        numOpsPerThread = workload.numOpsPerWU / numThreads,
        sampler = sampler.newInstance()))
  }

  /**
    * Initialize JMeter Environment
    *
    * @param JMETER_HOME Full path to JMeter home (ex: /home/centos/apache-jmeter-3.3)
    */
  def initJMeterEnvironment(JMETER_HOME: String): Unit = {
    import org.apache.jmeter.util.JMeterUtils._
    setJMeterHome(JMETER_HOME)
    loadJMeterProperties("%s/bin/jmeter.properties".format(JMETER_HOME))
    initLocale()
  }

  def getThreadGroupAndSamplerForWorkload(name: String, numWU: Int, numThreads: Int, numOpsPerThread: Int,
                                          sampler: Sampler): (ThreadGroup, Sampler) = {
    //Sampler
    sampler.setName(name)
    sampler.setProperty("numOpsPerThread", numOpsPerThread)

    // Setup Loop controller
    var loopCtrl = new LoopController
    loopCtrl.setLoops(numWU)
    loopCtrl.setFirst(true)

    // Setup Thread group
    var threadGroup = new ThreadGroup
    threadGroup.setNumThreads(numThreads)
    threadGroup.setRampUp(1)
    threadGroup.setSamplerController(loopCtrl)

    (threadGroup, sampler)
  }
}

// Workload settings - WU => Workload Unit
class Workload(val desc: String, val threadChoices: List[Int], val numOpsPerWU: Int, val numWU: Int) {}

//Command line validation
class BenchmarkingArgParser(arguments: Seq[String], validDatabases: List[String]) extends ScallopConf(arguments) {

  type arg[T] = ScallopOption[T]

  val databaseType: arg[String] = opt[String](required = true, short = 'd',
    descr = "Database to test (ex: cassandra, mongodb etc.,)")
  val connectionString: arg[String] = opt[String](required = true, short = 'c',
    descr =
      """Database Connection Params:
        |For Cassandra, list of IPs: 192.168.0.1,192.168.0.2
        |For MongoDB, connection URL: mongodb://username:password@db1.example.net:27017,db2.example.net:27017
      """.stripMargin)
  val schemaName: arg[String] = opt[String](required = true, short = 's',
    descr = "Schema or keyspace to run the tests (ex: accessioning)")
  val variantTableName: arg[String] = opt[String](required = true, short = 't',
    descr = "Variant table on which to run the tests (ex: global_variant_lkp)")
  val JMeterHome: arg[String] = opt[String](required = true, short = 'j',
    descr = "Full path to the JMeter installation directory (ex: /opt/jmeter)")
  val outputFile: arg[String] = opt[String](required = true, short = 'o',
    descr = "Path to store the test output (ex: /opt/cassandra_test/results.jtl)")

  validate(databaseType) { (databaseName) =>
    if (validDatabases.contains(databaseName)) Right(Unit)
    else Left("Database name must be one of %s".format(validDatabases.mkString(",")))
  }
  verify()
}