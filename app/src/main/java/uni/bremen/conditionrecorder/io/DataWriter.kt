package uni.bremen.conditionrecorder.io

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter


class DataWriter(file: File) : AutoCloseable {

    private val printer = CSVPrinter(
            OutputStreamWriter(FileOutputStream(file)),
            CSVFormat.DEFAULT.withHeader("T", "CH0", "CH1", "CH2", "CH3", "CH4", "CH5", "HR", "PHASE"))

    override fun close() {
        printer.flush()
        printer.close()
    }

    fun write(data:Array<*>) {
        printer.printRecord(System.currentTimeMillis(), *data)
    }

}