package com.rhosys.kineticjewelry.ui.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhosys.kineticjewelry.bluetooth.FakeBluetoothController
import com.rhosys.kineticjewelry.bluetooth.VibrationCommand
import com.rhosys.kineticjewelry.domain.usecase.ProcessNotificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothSimulatorViewModel @Inject constructor(
    private val fakeController: FakeBluetoothController,
    private val processNotification: ProcessNotificationUseCase,
) : ViewModel() {

    val commandLog: StateFlow<List<VibrationCommand>> = fakeController.log

    fun inject(packageName: String, contactName: String) {
        viewModelScope.launch {
            processNotification.execute(
                packageName = packageName,
                groupName = "",
                contactName = contactName.ifBlank { null },
            )
        }
    }

    fun clearLog() = fakeController.clearLog()
}
