package geometry

import org.openrndr.*
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector
import boofcv.struct.image.GrayF32
import boofcv.factory.feature.tracker.FactoryPointTracker
import georegression.struct.homography.Homography2D_F64
import boofcv.factory.sfm.FactoryMotion2D
import boofcv.abst.sfm.d2.PlToGrayMotion2D
import boofcv.alg.sfm.d2.StitchingFromMotion2D
import boofcv.struct.image.Planar
import boofcv.struct.image.ImageType
import boofcv.io.image.SimpleImageSequence
import boofcv.io.wrapper.DefaultMediaManager

import georegression.struct.point.Point2D_F64
import org.openrndr.color.ColorRGBa

import org.openrndr.xboofcv.*


class VideoMosaic : Program() {

    lateinit var video: SimpleImageSequence<Planar<GrayF32>>
    lateinit var stitch: StitchingFromMotion2D<Planar<GrayF32>, Homography2D_F64>
    var enlarged = false


    override fun setup() {
        val confDetector = ConfigGeneralDetector()
        confDetector.threshold = 1f
        confDetector.maxFeatures = 300
        confDetector.radius = 3
        // Use a KLT tracker
        val tracker = FactoryPointTracker.klt(intArrayOf(1, 2, 4, 8), confDetector, 3,
                GrayF32::class.java, GrayF32::class.java)

        // This estimates the 2D image motion
        // An Affine2D_F64 model also works quite well.
        val motion2D = FactoryMotion2D.createMotion2D(220, 3.0, 2, 30, 0.6, 0.5, false, tracker, Homography2D_F64())
        // wrap it so it output color images while estimating motion from gray
        val motion2DColor = PlToGrayMotion2D(motion2D, GrayF32::class.java)


        // This fuses the images together
        stitch = FactoryMotion2D.createVideoStitch(0.5, motion2DColor, ImageType.pl(3, GrayF32::class.java))

        // Load an image sequence
        val media = DefaultMediaManager.INSTANCE
        val fileName = "data/airplane01.mjpeg"
        video = media.openVideo(fileName, ImageType.pl(3, GrayF32::class.java))
        val frame = video.next()


        var shrink = Homography2D_F64(0.5, 0.0, frame.width / 4.0, 0.0, 0.5, frame.height / 4.0, 0.0, 0.0, 1.0)
        shrink = shrink.invert(null)

        // The mosaic will be larger in terms of pixels but the image will be scaled down.
        // To change this into stabilization just make it the same size as the input with no shrink.
        stitch.configure(frame.width, frame.height, shrink)

        // process the first frame
        stitch.process(frame)
    }

    override fun draw() {
        val frame = video.next()
        stitch.process(frame)

        var corners = stitch.getImageCorners(frame.width, frame.height, null)
        val a = colorBufferFromPlanarF32(stitch.stitchedImage)
        drawer.image(a)
        a.destroy()

        if (nearBorder(corners.p0, stitch) || nearBorder(corners.p1, stitch) ||
                nearBorder(corners.p2, stitch) || nearBorder(corners.p3, stitch)) {
            stitch.setOriginToCurrent()

            // only enlarge the image once
            if (!enlarged) {
                enlarged = true
                // double the image size and shift it over to keep it centered
                val widthOld = stitch.stitchedImage.width
                val heightOld = stitch.stitchedImage.height

                val widthNew = widthOld * 2
                val heightNew = heightOld * 2

                val tranX = (widthNew - widthOld) / 2
                val tranY = (heightNew - heightOld) / 2

                val newToOldStitch = Homography2D_F64(1.0, 0.0, (-tranX).toDouble(), 0.0, 1.0, (-tranY).toDouble(), 0.0, 0.0, 1.0)

                stitch.resizeStitchImage(widthNew, heightNew, newToOldStitch)
            }
        }
        corners = stitch.getImageCorners(frame.width, frame.height, null)
        drawer.stroke = ColorRGBa.PINK
        drawer.lineSegment(corners.p0.vector2, corners.p1.vector2)
        drawer.lineSegment(corners.p1.vector2, corners.p2.vector2)
        drawer.lineSegment(corners.p2.vector2, corners.p3.vector2)
        drawer.lineSegment(corners.p3.vector2, corners.p0.vector2)
    }
}

private fun nearBorder(p: Point2D_F64, stitch: StitchingFromMotion2D<*, *>): Boolean {
    val r = 10
    if (p.x < r || p.y < r)
        return true
    if (p.x >= stitch.getStitchedImage().width - r)
        return true
    return if (p.y >= stitch.getStitchedImage().height - r) true else false

}

fun main(args: Array<String>) {
    application(VideoMosaic(),
            configuration {

            })
}