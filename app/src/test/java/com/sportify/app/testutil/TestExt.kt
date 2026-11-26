package com.sportify.app.testutil

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * ViewModels expose state with stateIn(WhileSubscribed): the flow only runs while someone
 * collects it. This starts a collector that is cancelled automatically when the test ends.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.collectIn(flow: Flow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { } }
}
