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

import org.openrndr.xboofcv.*

class VideoStabilization : Program() {

    lateinit var video: SimpleImageSequence<Planar<GrayF32>>
    lateinit var stabilize: StitchingFromMotion2D<Planar<GrayF32>, Homography2D_F64>

    override fun setup() {
        val confDetector = ConfigGeneralDetector()
        confDetector.threshold = 10f
        confDetector.maxFeatures = 300
        confDetector.radius = 2
        // Use a KLT tracker
        val tracker = FactoryPointTracker.klt(intArrayOf(1, 2, 4, 8), confDetector, 3,
                GrayF32::class.java, GrayF32::class.java)

        // This estimates the 2D image motion
        // An Affine2D_F64 model also works quite well.
        val motion2D = FactoryMotion2D.createMotion2D(220, 3.0, 2, 30, 0.6, 0.5, false, tracker, Homography2D_F64())

        // wrap it so it output color images while estimating motion from gray
        val motion2DColor = PlToGrayMotion2D(motion2D, GrayF32::class.java)

        // This fuses the images together
        stabilize = FactoryMotion2D.createVideoStitch(0.5, motion2DColor, ImageType.pl(3, GrayF32::class.java))

        // Load an image sequence
        val media = DefaultMediaManager.INSTANCE
        val fileName = "data/shake.mjpeg"
        video = media.openVideo(fileName, ImageType.pl(3, GrayF32::class.java))
        val frame = video.next()

        // The mosaic will be larger in terms of pixels but the image will be scaled down.
        // To change this into stabilization just make it the same size as the input with no shrink.
        stabilize.configure(frame.width, frame.height, null)

        // process the first frame
        stabilize.process(frame)
    }

    override fun draw() {
        val frame = video.next()
        stabilize.process(frame)

        val a = colorBufferFromPlanarF32(stabilize.stitchedImage)
        drawer.image(a)
        a.destroy()
    }
}

fun main(args: Array<String>) {
    application(VideoStabilization(),
            configuration {

            })
}