package com.swiften.commonview.utils

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class CommonUtils {
  companion object {
    private const val BUFFER_SIZE = 8192

    /**
     * Reads all bytes from an input stream and writes them to an output stream.
     * https://stackoverflow.com/questions/43157/easy-way-to-write-contents-of-a-java-inputstream-to-an-outputstream
     */
    @Throws(IOException::class)
    fun transferInputToOutput(source: InputStream, sink: OutputStream): Long {
      var nread = 0L
      val buf = ByteArray(BUFFER_SIZE)
      var n: Int
      while (source.read(buf).also { n = it } > 0) {
        sink.write(buf, 0, n)
        nread += n.toLong()
      }
      return nread
    }
  }
}
