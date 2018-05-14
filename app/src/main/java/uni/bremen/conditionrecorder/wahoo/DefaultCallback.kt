package uni.bremen.conditionrecorder.wahoo

import com.wahoofitness.connector.HardwareConnector
import com.wahoofitness.connector.HardwareConnectorEnums
import com.wahoofitness.connector.HardwareConnectorTypes
import com.wahoofitness.connector.conn.connections.SensorConnection

open class DefaultCallback : HardwareConnector.Callback {

    override fun disconnectedSensor(p0: SensorConnection?) {

    }

    override fun connectedSensor(p0: SensorConnection?) {

    }

    override fun onFirmwareUpdateRequired(p0: SensorConnection?, p1: String?, p2: String?) {

    }

    override fun connectorStateChanged(p0: HardwareConnectorTypes.NetworkType?, p1: HardwareConnectorEnums.HardwareConnectorState?) {

    }

    override fun hasData() {

    }

}