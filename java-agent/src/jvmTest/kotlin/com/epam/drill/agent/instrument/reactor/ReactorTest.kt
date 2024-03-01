package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.request.RequestHolder
import com.epam.drill.common.agent.request.DrillRequest
import org.junit.Before
import reactor.core.publisher.Flux
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import java.time.Duration
import kotlin.test.Test

class ReactorTest {

    @Before
    fun setUp() {
        //Reset all installed hooks
        Hooks.resetOnEachOperator()
        Schedulers.resetOnScheduleHooks()

        //Init thread local instance
        RequestHolder.init(false)  //don't use TransmittableThreadLocal

        // InheritableThreadLocal / TransmittableThreadLocal in RequestHolder
        // enable propagating context to child threads
        // Warm-up creates child thread in advance, when no drill context is present in thread local yet
        // later this thread w/o initial context is re-used when calling Schedulers.single()
        // it won't work in Schedulers.parallel()
        Mono.just("warm-up")
            .subscribeOn(Schedulers.single())
            .block()
    }

    @Test
    fun `given Mono class, onAssembly must propagate test session to another threads`() {
        //Use Reactor Hooks to replace the Mono class with a decorator one
        Hooks.onEachOperator {
            PublisherAssembler.onAssembly(it, Mono::class.java) as Mono<Any>?
        }

        //Save testSession in the current thread
        RequestHolder.store(DrillRequest("test-session"))
        try {
            val mono = Mono.just("test")
                .subscribeOn(Schedulers.single())
                .map {
                    //Retrieve testSession in subscriber thread
                    RequestHolder.retrieve()?.drillSessionId
                }

            StepVerifier.create(mono)
                //Check that testSession was propagated to subscriber thread
                .expectNext("test-session")
                .expectComplete()
                .verify()
        } finally {
            RequestHolder.remove()
        }
    }

    @Test
    fun `given Flux class, onAssembly must propagate test session to another threads`() {
        val testSession = "session-1"

        //Use Reactor Hooks to replace the Flux class with a decorator one
        Hooks.onEachOperator {
            PublisherAssembler.onAssembly(it, Flux::class.java) as Flux<Any>?
        }

        //Save testSession in the current thread
        RequestHolder.store(DrillRequest(testSession))
        try {
            val flux = Flux.just("test-1", "test-2", "test-3")
                .subscribeOn(Schedulers.single())
                .filter {
                    //Filter data with testSession in subscriber thread
                    RequestHolder.retrieve()?.drillSessionId == testSession
                }
                .map {
                    //Retrieve testSession in subscriber thread
                    it + "-" + RequestHolder.retrieve()?.drillSessionId
                }

            StepVerifier.create(flux)
                //Check that testSession was propagated to subscriber thread
                .expectNext("test-1-$testSession", "test-2-$testSession", "test-3-$testSession")
                .expectComplete()
                .verify()
        } finally {
            RequestHolder.remove()
        }
    }

    @Test
    fun `given MonoDelay class, PropagatedDrillRequestRunnable must propagate test session to scheduled task`() {
        //Use Reactor Schedulers Hook to replace the Runnable class with a decorator one
        Schedulers.onScheduleHook("test-hook") { runnable ->
            RequestHolder.retrieve()?.let { PropagatedDrillRequestRunnable(it, runnable) } ?: runnable
        }

        //Save testSession in the current thread
        RequestHolder.store(DrillRequest("test-session"))
        try {
            val mono = Mono.just("test")
                .delaySubscription(Duration.ofMillis(10), Schedulers.single())
                .map {
                    //Retrieve testSession in a scheduled task
                    RequestHolder.retrieve()?.drillSessionId
                }

            StepVerifier.create(mono)
                //Check that testSession was propagated to subscriber thread
                .expectNext("test-session")
                .expectComplete()
                 .verify()
        } finally {
            RequestHolder.remove()
        }
    }
}