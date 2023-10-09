package com.xemantic.art.theremin

import com.xemantic.osc.*
import com.xemantic.osc.convert.oscDecoders
import com.xemantic.osc.convert.oscEncoders
import com.xemantic.osc.udp.UdpOscTransport
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.math.*
import kotlin.reflect.KType

class UdpOsc(
  val port: Int = 0,
  val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : Extension {

  override var enabled: Boolean = true

  var encoders: Map<KType, OscEncoder<*>> = coreEncoders

  val input: OscInput = OscInput(scope) {
    decoders += coreDecoders
  }

  var encodeVectorsAsFloats = true

  var decodeFloatsAsVectors = true

  private val selectorManager: SelectorManager = SelectorManager()
  private val socket = aSocket(selectorManager).udp().bind(
    InetSocketAddress(
      hostname = "::",
      port = port
    )
  )

  private val transport: OscTransport = UdpOscTransport(socket)

  override fun setup(program: Program) {
    input.decoders += if (decodeFloatsAsVectors) {
      floatsToVectorDecoders
    } else {
      doublesToVectorDecoders
    }
    encoders += if (encodeVectorsAsFloats) {
      vectorToFloatsEncoders
    } else {
      vectorToDoublesEncoders
    }
    scope.launch {
      input.connect(transport)
    }
  }

  override fun shutdown(program: Program) {
    socket.close()
    selectorManager.close()
    scope.cancel()
  }

  fun output(hostname: String, port: Int): OscOutput {
    val output = transport.output(
      hostname, port
    )
    // TODO why not setting it up as reference?
    output.encoders += encoders
    return output
  }

//  fun launch(
//      block: suspend CoroutineScope.(input: OscInput) -> Unit
//  ): Job = scope.launch {
//    block(input)
//  }

}

val coreDecoders = oscDecoders {
  decoder { assertTypeTag("ii"); IntVector2(int(), int()) }
  decoder { assertTypeTag("iii"); IntVector3(int(), int(), int()) }
  decoder { assertTypeTag("iiii"); IntVector4(int(), int(), int(), int()) }
}

val coreEncoders: Map<KType, OscEncoder<*>> = oscEncoders {
  encoder<IntVector2> {
    typeTag("ii")
    int(it.x)
    int(it.y)
  }
  encoder<IntVector3> {
    typeTag("iii")
    int(it.x)
    int(it.y)
    int(it.z)
  }
  encoder<IntVector4> {
    typeTag("iiii")
    int(it.x)
    int(it.y)
    int(it.z)
    int(it.w)
  }
}

@OptIn(ExperimentalUnsignedTypes::class)
val colorRgbaToOscColorEncoder: OscEncoder<ColorRGBa> = {
  typeTag("r")
  ubyte((it.r * 255.0).toInt().toUByte())
  ubyte((it.g * 255.0).toInt().toUByte())
  ubyte((it.b * 255.0).toInt().toUByte())
  ubyte((it.alpha * 255.0).toInt().toUByte())
}

//val oscColorToColorRgbaDecoder: OscDecoder<ColorRGBa> = {
////  assertTypeTag("r")
//    @Suppress("OPT_IN_USAGE")
//  ColorRGBa(
//    r = ubyte().toDouble() / 255.0,
//    g = ubyte().toDouble() / 255.0,
//    b = ubyte().toDouble() / 255.0,
//    alpha = ubyte().toDouble() / 255.0
//  )
//}

val floatsToVectorDecoders = oscDecoders {
  decoder {
    assertTypeTag("ff")
    Vector2(
      float().toDouble(),
      float().toDouble()
    )
  }
  decoder {
    assertTypeTag("fff")
    Vector3(
      float().toDouble(),
      float().toDouble(),
      float().toDouble()
    )
  }
  decoder {
    assertTypeTag("ffff")
    Vector4(
      float().toDouble(),
      float().toDouble(),
      float().toDouble(),
      float().toDouble()
    )
  }
}

val doublesToVectorDecoders = oscDecoders {
  decoder {
    assertTypeTag("dd")
    Vector2(
      double(),
      double()
    )
  }
  decoder {
    assertTypeTag("ddd")
    Vector3(
      double(),
      double(),
      double()
    )
  }
  decoder {
    assertTypeTag("dddd")
    Vector4(
      double(),
      double(),
      double(),
      double()
    )
  }
}

val vectorToFloatsEncoders: Map<KType, OscEncoder<*>> = oscEncoders {
  encoder<Vector2> {
    typeTag("ff")
    float(it.x.toFloat())
    float(it.y.toFloat())
  }
  encoder<Vector3> {
    typeTag("fff")
    float(it.x.toFloat())
    float(it.y.toFloat())
    float(it.z.toFloat())
  }
  encoder<Vector4> {
    typeTag("ffff")
    float(it.x.toFloat())
    float(it.y.toFloat())
    float(it.z.toFloat())
    float(it.w.toFloat())
  }
  encoder<IntVector2> {
    typeTag("ii")
    int(it.x)
    int(it.y)
  }
  encoder<ColorRGBa> {
    typeTag("ffff")
    float(it.r.toFloat())
    float(it.g.toFloat())
    float(it.b.toFloat())
    float(it.alpha.toFloat())
  }
}

val vectorToDoublesEncoders: Map<KType, OscEncoder<*>> = oscEncoders {
  encoder<Vector2> {
    typeTag("ff")
    double(it.x)
    double(it.y)
  }
  encoder<Vector3> {
    typeTag("fff")
    double(it.x)
    double(it.y)
    double(it.z)
  }
  encoder<Vector4> {
    typeTag("ffff")
    double(it.x)
    double(it.y)
    double(it.z)
    double(it.w)
  }
  encoder<IntVector2> {
    typeTag("ii")
    int(it.x)
    int(it.y)
  }
  encoder<ColorRGBa> {
    typeTag("ffff")
    double(it.r)
    double(it.g)
    double(it.b)
    double(it.alpha)
  }
}
