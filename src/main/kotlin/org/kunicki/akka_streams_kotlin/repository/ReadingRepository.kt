package org.kunicki.akka_streams_kotlin.repository

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ResultSet
import net.javacrumbs.futureconverter.java8guava.FutureConverter.toCompletableFuture
import org.kunicki.akka_streams_kotlin.model.ValidReading
import java.util.concurrent.CompletionStage

class ReadingRepository {

    private val session = Cluster.builder().addContactPoint("127.0.0.1").build().connect()

    private val preparedStatement = session.prepare("insert into akka_streams.readings (id, value) values (?, ?)")

    fun save(validReading: ValidReading): CompletionStage<ResultSet> {
        val boundStatement = preparedStatement.bind(validReading.id, validReading.value.toFloat())
        return toCompletableFuture(session.executeAsync(boundStatement))
    }

    fun shutdown() = session.cluster.close()
}
