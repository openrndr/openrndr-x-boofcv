package org.openrndr.xboofcv

import boofcv.struct.image.GrayF32
import boofcv.struct.image.GrayU8

import boofcv.struct.image.Planar
import georegression.struct.point.Point2D_F64
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.math.Vector2


val Point2D_F64.vector2: Vector2
    get() {
        return Vector2(this.x, this.y)
    }


fun grayF32fromColorBuffer(colorBuffer: ColorBuffer): GrayF32 {
    val p = GrayF32(colorBuffer.width, colorBuffer.height)
    colorBuffer.shadow.download()

    var offset = 0
    for (y in 0 until colorBuffer.height) {
        for (x in 0 until colorBuffer.width) {
            val c = colorBuffer.shadow.read(x, y)
            p.data[offset] = (c.r * 255).toFloat()
            offset++
        }
    }
    return p
}


fun planarF32fromColorBuffer(colorBuffer: ColorBuffer): Planar<GrayF32> {
    val p = Planar<GrayF32>(GrayF32::class.java, colorBuffer.width, colorBuffer.height, colorBuffer.format.componentCount)
    colorBuffer.shadow.download()

    val bands = p.bands

    var offset = 0
    for (y in 0 until colorBuffer.height) {
        for (x in 0 until colorBuffer.width) {
            val c = colorBuffer.shadow.read(x, y)
            bands[0].data[offset] = (c.r * 255).toFloat()
            bands[1].data[offset] = (c.g * 255).toFloat()
            bands[2].data[offset] = (c.b * 255).toFloat()
            offset++
        }
    }
    return p
}

fun colorBufferFromGrayF32(gray: GrayF32): ColorBuffer {
    val cb = colorBuffer(gray.width, gray.height, 1.0, ColorFormat.RGB, ColorType.FLOAT32)
    val shadow = cb.shadow
    shadow.buffer.rewind()
    var offset = 0
    for (y in 0 until gray.height) {
        for (x in 0 until gray.width) {
            val r = gray.data[offset].toDouble() / 255.0
            offset++
            shadow.write(x, y, ColorRGBa(r, r, r))
        }
    }
    shadow.upload()
    return cb
}

fun colorBufferFromGrayU8(gray: GrayU8): ColorBuffer {
    val cb = colorBuffer(gray.width, gray.height, 1.0, ColorFormat.RGB, ColorType.FLOAT32)
    val shadow = cb.shadow
    shadow.buffer.rewind()
    var offset = 0
    for (y in 0 until gray.height) {
        for (x in 0 until gray.width) {
            val r = gray.data[offset].toDouble() / 255.0
            offset++
            shadow.write(x, y, ColorRGBa(r, r, r))
        }
    }
    shadow.upload()
    return cb
}

fun colorBufferFromPlanarF32(planar: Planar<GrayF32>): ColorBuffer {
    val bandCount = planar.bands.size
    val format = when (bandCount) {
        3 -> ColorFormat.RGB
        4 -> ColorFormat.RGBa
        else -> TODO("not implemented")
    }

    val bands = planar.bands
    val cb = colorBuffer(planar.width, planar.height, 1.0, format, ColorType.FLOAT32)
    val shadow = cb.shadow
    shadow.buffer.rewind()
    var offset = 0
    for (y in 0 until planar.height) {
        for (x in 0 until planar.width) {
            val r = bands[0].data[offset].toDouble() / 255.0
            val g = bands[1].data[offset].toDouble() / 255.0
            val b = bands[2].data[offset].toDouble() / 255.0
            offset++
            shadow.write(x, y, ColorRGBa(r, g, b))
        }
    }
    shadow.upload()
    return cb
}

fun colorBufferFromPlanarU8(planar: Planar<GrayU8>): ColorBuffer {
    val bandCount = planar.bands.size
    val format = when (bandCount) {
        3 -> ColorFormat.RGBa
        4 -> ColorFormat.RGBa
        else -> TODO("not implemented")
    }

    val bands = planar.bands
    val cb = colorBuffer(planar.width, planar.height, 1.0, format, ColorType.UINT8)
    val shadow = cb.shadow
    shadow.buffer.rewind()
    var offset = 0
    for (y in 0 until planar.height) {
        for (x in 0 until planar.width) {
            val r = (bands[0].data[offset].toInt() and 0xff).toDouble() / 255.0
            val g = (bands[1].data[offset].toInt() and 0xff).toDouble() / 255.0
            val b = (bands[2].data[offset].toInt() and 0xff).toDouble() / 255.0
            offset++
            shadow.write(x, y, ColorRGBa(r, g, b))
        }
    }
    shadow.upload()
    return cb
}