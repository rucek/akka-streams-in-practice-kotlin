package org.kunicki.akka_streams_kotlin

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.IOResult
import akka.stream.javadsl.FileIO
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import io.vavr.collection.Stream
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletionStage

class RandomDataGenerator {

    private val logger = LoggerFactory.getLogger(RandomDataGenerator::class.java)

    private val system = ActorSystem.create("random-data-generator")
    private val materializer = ActorMaterializer.create(system)

    private val config = ConfigFactory.load()
    private val numberOfFiles = config.getInt("generator.number-of-files")
    private val numberOfPairs = config.getInt("generator.number-of-pairs")
    private val invalidLineProbability = config.getDouble("generator.invalid-line-probability")

    private val random = Random()

    private fun randomValue() =
        if (random.nextDouble() > invalidLineProbability) {
            random.nextDouble().toString()
        } else {
            "invalid_value"
        }

    fun generate(): CompletionStage<Void> {
        logger.info("Starting generation")

        return Source.range(1, numberOfFiles)
                .mapAsyncUnordered<IOResult>(numberOfFiles) { _ ->
                    val fileName = UUID.randomUUID().toString()
                    Source.range(1, numberOfPairs).map { i ->
                        val id = random.nextInt(1000000)
                        Stream.of(randomValue(), randomValue())
                                .map { v -> ByteString.fromString(id.toString() + ";" + v + "\n") }
                                .foldLeft(ByteString.empty(), { obj, that -> obj.concat(that) })
                    }
                    .runWith<CompletionStage<IOResult>>(FileIO.toPath(Paths.get("data", fileName + ".csv")), materializer)
                }
                .runWith<CompletionStage<Done>>(Sink.ignore<IOResult>(), materializer)
                .thenAccept { _ ->
                    logger.info("Generated random data")
                    system.terminate()
                }
    }
}

fun main(args: Array<String>) {
    RandomDataGenerator().generate()
}