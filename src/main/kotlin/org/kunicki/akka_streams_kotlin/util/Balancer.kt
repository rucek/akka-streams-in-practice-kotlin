package org.kunicki.akka_streams_kotlin.util

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.Graph
import akka.stream.javadsl.Balance
import akka.stream.javadsl.Flow
import akka.stream.javadsl.GraphDSL
import akka.stream.javadsl.Merge

object Balancer {

    fun <In, Out> create(numberOfWorkers: Int, worker: Flow<In, Out, NotUsed>): Graph<FlowShape<In, Out>, NotUsed> {
        return GraphDSL.create { builder ->
            val balance = builder.add(Balance.create<In>(numberOfWorkers))
            val merge = builder.add(Merge.create<Out>(numberOfWorkers))

            for (i in 0 until numberOfWorkers) {
                val workerStage = builder.add(worker)
                builder.from(balance.out(i)).via<Out>(workerStage).toInlet(merge.`in`(i))
            }

            FlowShape.of(balance.`in`(), merge.out())
        }
    }
}
