package geometry

import boofcv.alg.distort.RemovePerspectiveDistortion
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.configuration
import org.openrndr.draw.ColorBuffer
import org.openrndr.xboofcv.planarF32fromColorBuffer
import boofcv.struct.image.GrayF32
import boofcv.struct.image.ImageType
import georegression.struct.point.Point2D_F64
import org.openrndr.xboofcv.colorBufferFromPlanarF32


class RemovePerspectiveDistortionExample: Program() {

    lateinit var inputImage:ColorBuffer
    lateinit var outputImage:ColorBuffer

    override fun setup() {

        inputImage = ColorBuffer.fromUrl("file:data/goals_and_stuff.jpg")
        val input = planarF32fromColorBuffer(inputImage)
        val removePerspective = RemovePerspectiveDistortion(400, 500, ImageType.pl(3, GrayF32::class.java))
        if (!removePerspective.apply(input,
                        Point2D_F64(267.0, 182.0), Point2D_F64(542.0, 68.0),
                        Point2D_F64(519.0, 736.0), Point2D_F64(276.0, 570.0))) {
            throw RuntimeException("Failed!?!?")
        }

        outputImage = colorBufferFromPlanarF32(removePerspective.output)

    }

    override fun draw() {
        drawer.image(inputImage)
        drawer.translate(inputImage.width.toDouble(), 0.0)
        drawer.image(outputImage)
    }

}

fun main(args: Array<String>) {
    application(RemovePerspectiveDistortionExample(),
            configuration {
                width = 1280
                height = 720
            })
}