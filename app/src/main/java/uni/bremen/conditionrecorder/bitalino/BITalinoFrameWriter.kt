package uni.bremen.conditionrecorder.bitalino

import info.plux.pluxapi.bitalino.BITalinoFrame
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter


class BITalinoFrameWriter(file: File) : AutoCloseable {

    private val printer = CSVPrinter(
            OutputStreamWriter(FileOutputStream(file)),
            CSVFormat.DEFAULT.withHeader("ID", "Name", "Designation", "Company"))

    override fun close() {
        printer.flush()
        printer.close()
    }

    fun write(frame:BITalinoFrame) {
        printer.printRecord(frame.analogArray)
    }

}