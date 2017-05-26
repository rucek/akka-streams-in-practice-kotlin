package org.kunicki.akka_streams_kotlin.importer

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.kunicki.akka_streams_kotlin.model.InvalidReading
import org.kunicki.akka_streams_kotlin.model.Reading
import org.kunicki.akka_streams_kotlin.model.ValidReading
import org.kunicki.akka_streams_kotlin.repository.ReadingRepository
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

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
}
