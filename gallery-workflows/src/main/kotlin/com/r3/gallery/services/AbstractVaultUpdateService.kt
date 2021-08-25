package com.r3.gallery.services

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.ServiceLifecycleEvent
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.contextLogger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class AbstractVaultUpdateService<T: ContractState>(val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    /**
     * Config variable name that enables or disables this service.
     */
    abstract val configEnableString: String

    /**
     * The contractStateType to track updates for.
     */
    abstract val trackedContractStateType: Class<T>

    open var executor : ExecutorService? = null

    companion object {
        val logger = contextLogger()
    }

    init {
        serviceHub.register { processEvent(it) }
    }

    /**
     * [ServiceLifecycleEvent] event handler.
     * Wait for [ServiceLifecycleEvent.STATE_MACHINE_STARTED] event and call [start].
     * @param event the [ServiceLifecycleEvent] instance to process.
     */
    private fun processEvent(event: ServiceLifecycleEvent) {
        when (event) {
            ServiceLifecycleEvent.STATE_MACHINE_STARTED -> {
                start()
            }
            else -> {
                // Not interested in other events
            }
        }
    }

    /**
     * Start tracking updates if this service is enabled.
     */
    open fun start() {
        val config = serviceHub.getAppContext().config
        if (config.exists(configEnableString) && !config.getBoolean(configEnableString)) {
            logger.info("Service ${this::class.java} is disabled due to config")
        } else {
            logger.info("Starting ${this::class.java} service")
            executor = Executors.newSingleThreadExecutor()
            trackUpdates()
        }
    }

    /**
     * Track [ContractState] updates to the vault for contract type [trackedContractStateType].
     * Updates are filtered with [filterUpdates] and processed with [processUpdate].
     */
    open fun trackUpdates() {
        serviceHub.vaultService.trackBy(trackedContractStateType)
            .updates.subscribe { update ->
            filterUpdates(update.produced.map { it })
                .forEach {
                    processUpdate(it)
                }
        }
    }

    /**
     * Filter [StateAndRef<ContractState>] updates and return a list of updates that should be processed by
     * [processUpdate].
     * @param stateAndRefs list of [ContractState] vault updates to filter
     * @return list of filtered [StateAndRef<ContractState>].
     */
    abstract fun filterUpdates(stateAndRefs: List<StateAndRef<ContractState>>): List<StateAndRef<ContractState>>

    /**
     * Process a single vault [ContractState] update.
     * @param stateAndRef the [ContractState] state update to process.
     */
    abstract fun processUpdate(stateAndRef: StateAndRef<ContractState>)
}