package geometry

import org.openrndr.Program
import boofcv.io.UtilIO
import boofcv.io.image.UtilImageIO
import boofcv.struct.image.GrayU8
import boofcv.io.image.ConvertBufferedImage
import boofcv.io.calibration.CalibrationIO
import georegression.struct.se.Se3_F64
import boofcv.alg.interpolate.InterpolationType
import boofcv.alg.sfm.overhead.CreateSyntheticOverheadViewPL
import boofcv.alg.sfm.overhead.SelectOverheadParameters
import boofcv.struct.calib.StereoParameters
import boofcv.struct.image.Planar
import org.openrndr.application
import org.openrndr.configuration
import org.openrndr.draw.ColorBuffer
import org.openrndr.xboofcv.colorBufferFromPlanarU8


class OverheadView: Program() {


    lateinit var result:ColorBuffer
    override fun setup() {
        val input = UtilImageIO.loadImage("data/road/left01.png")
        val imageRGB = ConvertBufferedImage.convertFromPlanar(input, null, true, GrayU8::class.java)
        val stereoParam = CalibrationIO.load<Any>("data/road/stereo01.yaml") as StereoParameters
        val groundToLeft = CalibrationIO.load<Se3_F64>("data/road/ground_to_left_01.yaml") as Se3_F64
        val generateOverhead = CreateSyntheticOverheadViewPL(InterpolationType.BILINEAR, 3, GrayU8::class.java)

        val cellSize = 0.05
        val selectMapSize = SelectOverheadParameters(cellSize, 20.0, 0.5)
        selectMapSize.process(stereoParam.left, groundToLeft)
        val overheadWidth = selectMapSize.overheadWidth
        val overheadHeight = selectMapSize.overheadHeight

        val overheadRGB = Planar(GrayU8::class.java, overheadWidth, overheadHeight, 3)
        generateOverhead.configure(stereoParam.left,groundToLeft,
                selectMapSize.getCenterX(), selectMapSize.getCenterY(), cellSize,overheadRGB.width,overheadRGB.height);
        generateOverhead.process(imageRGB, overheadRGB);

        result = colorBufferFromPlanarU8(overheadRGB)

    }

    override fun draw() {
        drawer.image(result)
    }
}

fun main(args: Array<String>) {
    application(OverheadView(),
            configuration {

            })
}