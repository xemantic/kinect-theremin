package com.xemantic.art.theremin

import com.xemantic.osc.OscOutput
import com.xemantic.osc.route
import kotlinx.coroutines.Dispatchers
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.depth.camera.DepthMeasurement
import org.openrndr.extra.fx.colormap.TurboColormap
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.kinect.v1.Kinect1
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.TextParameter
import org.openrndr.launch
import org.openrndr.math.Vector2
import org.openrndr.math.map
import java.lang.Exception

class FindMinDepthFilter : Filter(
  watcher = filterWatcherFromUrl(
    "file:src/main/resources/find-min-depth.frag"
  )
) {
  var iWidth: Int by parameters
  var iHeight: Int by parameters
}

fun main() = application {

  configure {
    width = 200 /* UI */ + 640 /* Kinect */ + 100 /* signal level */
    height = 480
  }

  program {
    val osc = extend(UdpOsc())
    var oscOutput: OscOutput? = null

    val depthParameters = object {

      @DoubleParameter(label = "min", low = .3, high = 4.0, order = 0)
      var min: Double = .3

      @DoubleParameter(label = "max", low = .3, high = 4.0, order = 1)
      var max: Double = 1.0

      @BooleanParameter(label = "mirror", order = 2)
      var mirror: Boolean = true

    }

    val oscParameters = object {

      @TextParameter(label = "host", order = 0)
      var host: String = "localhost"

      @TextParameter(label = "port", order = 1)
      var port: String = "12345"

      @ActionParameter(label = "Stream/Stop", order = 2)
      fun stream() {
        if (oscOutput != null) {
          oscOutput!!.close()
          oscOutput = null
          status = ""
        } else {
          try {
            val port = this.port.toInt()
            check(port in 1..65535) {
              "port range: 1..65535"
            }
            oscOutput = osc.output(host, port).apply {
              route<Float>("/frequency")
            }
            status = "streaming..."
          } catch (e : Exception) {
            status = e.message!!
          }
        }
      }

      @TextParameter(label = "status", order = 3)
      var status: String = ""

    }

    val gui = GUI()
    gui.compartmentsCollapsedByDefault = false
    gui.add(depthParameters, "depth")
    gui.add(oscParameters, "OSC")
    extend(gui)
    oscParameters.status = ""
    val kinect = extend(Kinect1())
    val camera = kinect.openDevice().depthCamera.apply {
      enabled = true
      depthMeasurement = DepthMeasurement.METERS
    }
    val minDepthOutputBuffer = colorBuffer(
      width = 1,
      height = 1,
      format = ColorFormat.RGBa,
      type = ColorType.FLOAT32
    ).apply {
      filterMag = MagnifyingFilter.NEAREST
      filterMin = MinifyingFilter.NEAREST
    }
    val turboColorBuffer = colorBuffer(width = 640, height = 480)

    val filter = FindMinDepthFilter().apply {
      iWidth = camera.resolution.x
      iHeight = camera.resolution.y
    }

    val shadow = minDepthOutputBuffer.shadow

    val turboColormap = TurboColormap()

    extend {
      camera.flipH = depthParameters.mirror
      filter.apply(camera.currentFrame, minDepthOutputBuffer)

      turboColormap.apply {
        minValue = depthParameters.min
        maxValue = depthParameters.max
      }
      shadow.download()
      val depth = shadow[0, 0].r
      val signal = map(
        depthParameters.min,
        depthParameters.max,
        0.0,
        1.0,
        depth,
        clamp = true
      )
      if (oscOutput != null) {
        launch(Dispatchers.IO) {
          oscOutput!!.send("/frequency", signal)
        }
      }
      turboColormap.apply(camera.currentFrame, turboColorBuffer)
      drawer.image(
        colorBuffer = turboColorBuffer,
        position = Vector2(200.0, 0.0),
      )
      drawer.fill = ColorRGBa.PINK
      drawer.rectangle(
        x = 200.0 + 640.0,
        y = 0.0,
        width = 100.0,
        height = signal * height.toDouble()
      )
    }
  }
}
