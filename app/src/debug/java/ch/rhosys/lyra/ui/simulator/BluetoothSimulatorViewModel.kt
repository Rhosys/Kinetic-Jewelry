package ch.rhosys.lyra.ui.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.bluetooth.FakeBluetoothController
import ch.rhosys.lyra.bluetooth.VibrationCommand
import ch.rhosys.lyra.domain.usecase.ProcessNotificationUseCase
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
