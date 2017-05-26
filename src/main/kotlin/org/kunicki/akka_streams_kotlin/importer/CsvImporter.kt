package org.kunicki.akka_streams_kotlin.importer

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.kunicki.akka_streams_kotlin.repository.ReadingRepository
import org.slf4j.LoggerFactory
import java.nio.file.Paths

class CsvImporter(config: Config,
                  readingRepository: ReadingRepository,
                  val system: ActorSystem) {

    private val logger = LoggerFactory.getLogger(CsvImporter::class.java)

    private val importDirectory = Paths.get(config.getString("importer.import-directory")).toFile()
    private val linesToSkip = config.getLong("importer.lines-to-skip")
    private val concurrentFiles = config.getInt("importer.concurrent-files")
    private val concurrentWrites = config.getInt("importer.concurrent-writes")
    private val nonIOParallelism = config.getInt("importer.non-io-parallelism")
}
