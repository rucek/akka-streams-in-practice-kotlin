package org.kunicki.akka_streams_kotlin.importer

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.javadsl.Flow
import akka.stream.javadsl.Framing
import akka.stream.javadsl.FramingTruncation
import akka.stream.javadsl.StreamConverters
import akka.util.ByteString
import com.typesafe.config.Config
import org.kunicki.akka_streams_kotlin.model.InvalidReading
import org.kunicki.akka_streams_kotlin.model.Reading
import org.kunicki.akka_streams_kotlin.model.ValidReading
import org.kunicki.akka_streams_kotlin.repository.ReadingRepository
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.zip.GZIPInputStream

class CsvImporter(config: Config,
                  readingRepository: ReadingRepository,
                  val system: ActorSystem) {

    private val logger = LoggerFactory.getLogger(CsvImporter::class.java)

    private val importDirectory = Paths.get(config.getString("importer.import-directory")).toFile()
    private val linesToSkip = config.getLong("importer.lines-to-skip")
    private val concurrentFiles = config.getInt("importer.concurrent-files")
    private val concurrentWrites = config.getInt("importer.concurrent-writes")
    private val nonIOParallelism = config.getInt("importer.non-io-parallelism")

    fun parseLine(line: String): CompletionStage<Reading> {
        return CompletableFuture.supplyAsync {
            val fields = line.split(";")
            val id = fields.first().toInt()

            try {
                val value = fields.last().toDouble()
                ValidReading(id, value)
            } catch (t: Throwable) {
                logger.error("Unable to parse line: {}: {}", line, t.message)
                InvalidReading(id)
            }
        }
    }

    val lineDelimiter: Flow<ByteString, ByteString, NotUsed> =
            Framing.delimiter(ByteString.fromString("\n"), 128, FramingTruncation.ALLOW)

    val parseFile: Flow<File, Reading, NotUsed> =
            Flow.create<File>().flatMapConcat { file ->
                val inputStream = GZIPInputStream(FileInputStream(file))
                StreamConverters.fromInputStream { inputStream }
                        .via(lineDelimiter)
                        .drop(linesToSkip)
                        .map { it.utf8String() }
                        .mapAsync(nonIOParallelism, this::parseLine)
            }

    val computeAverage: Flow<Reading, ValidReading, NotUsed> =
            Flow.create<Reading>().grouped(2).mapAsyncUnordered(nonIOParallelism, { readings ->
                CompletableFuture.supplyAsync {
                    val validReadings = readings.filterIsInstance<ValidReading>()
                    val average = if (validReadings.isNotEmpty()) validReadings.map { it.value }.average() else -1.0
                    ValidReading(readings.first().id, average)
                }
            })
}
