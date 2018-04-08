package tracking

import org.openrndr.Program

import boofcv.struct.image.GrayF32
import boofcv.struct.image.ImageType
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector
import boofcv.abst.sfm.d2.ImageMotion2D
import boofcv.alg.background.moving.BackgroundMovingBasic
import boofcv.factory.feature.tracker.FactoryPointTracker

import georegression.struct.homography.Homography2D_F64
import boofcv.factory.sfm.FactoryMotion2D

import boofcv.factory.background.ConfigBackgroundGmm
import boofcv.factory.background.ConfigBackgroundGaussian
import boofcv.factory.background.ConfigBackgroundBasic
import boofcv.alg.distort.PointTransformHomography_F32
import boofcv.factory.background.FactoryBackgroundModel
import georegression.struct.homography.Homography2D_F32
import boofcv.io.wrapper.DefaultMediaManager
import boofcv.struct.image.GrayU8
import org.ejml.ops.ConvertMatrixData
import boofcv.core.image.GConvertImage
import boofcv.io.image.SimpleImageSequence
import org.openrndr.application
import org.openrndr.configuration
import org.openrndr.draw.grayscale
import org.openrndr.math.Matrix55
import org.openrndr.xboofcv.colorBufferFromGrayF32
import org.openrndr.xboofcv.colorBufferFromGrayU8


class BackgroundRemovalMoving : Program() {
    var fileName = "data/tracking/chipmunk.mjpeg"
    var imageType = ImageType.single(GrayF32::class.java)
    lateinit var background: BackgroundMovingBasic<GrayF32, Homography2D_F32>

    lateinit var motion2D: ImageMotion2D<GrayF32, Homography2D_F64>
    lateinit var video: SimpleImageSequence<GrayF32>
    lateinit var firstToCurrent32: Homography2D_F32
    lateinit var grey: GrayF32
    lateinit var segmented: GrayU8

    override fun setup() {

        val confDetector = ConfigGeneralDetector()
        confDetector.threshold = 10f
        confDetector.maxFeatures = 300
        confDetector.radius = 6


        val tracker = FactoryPointTracker.klt<GrayF32, GrayF32>(intArrayOf(1, 2, 4, 8), confDetector, 3,
                GrayF32::class.java, null)

        motion2D = FactoryMotion2D.createMotion2D(500, 0.5, 3, 100, 0.6, 0.5, false, tracker, Homography2D_F64())

        val configBasic = ConfigBackgroundBasic(30f, 0.005f)

        // Configuration for Gaussian model.  Note that the threshold changes depending on the number of image bands
        // 12 = gray scale and 40 = color
        val configGaussian = ConfigBackgroundGaussian(12f, 0.001f)
        configGaussian.initialVariance = 64f
        configGaussian.minimumDifference = 5f

        // Note that GMM doesn't interpolate the input image. Making it harder to model object edges.
        // However it runs faster because of this.
        val configGmm = ConfigBackgroundGmm()
        configGmm.initialVariance = 1600f
        configGmm.significantWeight = 1e-1f

        background = FactoryBackgroundModel.movingBasic<GrayF32, Homography2D_F32>(configBasic, PointTransformHomography_F32(), imageType)
        background.unknownValue = 1

        val media = DefaultMediaManager.INSTANCE
        video = media.openVideo(fileName, background.imageType)
        //media.openCamera(null,640,480,background.getImageType());

        // storage for segmented image.  Background = 0, Foreground = 1
        segmented = GrayU8(video.nextWidth, video.nextHeight)
        // Grey scale image that's the input for motion estimation
        grey = GrayF32(segmented.width, segmented.height)

        firstToCurrent32 = Homography2D_F32()
        val homeToWorld = Homography2D_F32()
        homeToWorld.a13 = grey.width / 2.0f
        homeToWorld.a23 = grey.height / 2.0f

        background.initialize(grey.width * 2, grey.height * 2, homeToWorld);
        var fps = 0.0
        val alpha = 0.01 // smoothing factor for FPS


    }

    override fun draw() {
        if (video.hasNext()) {
            val input = video.next()
            GConvertImage.convert(input, grey)

            if (!motion2D.process(grey)) {
                throw RuntimeException("Should handle this scenario")
            }

            val firstToCurrent64 = motion2D.firstToCurrent
            ConvertMatrixData.convert(firstToCurrent64, firstToCurrent32)

            background.segment(firstToCurrent32, input, segmented)
            background.updateBackground(firstToCurrent32, input)
            val cb = colorBufferFromGrayF32(input)
            drawer.image(cb)
            cb.destroy()

            drawer.translate(cb.width.toDouble(), 0.0)

            val cb2 = colorBufferFromGrayU8(segmented)
            drawer.drawStyle.colorMatrix = grayscale(100.0, 100.0, 100.0)
            drawer.image(cb2)
            cb2.destroy()

        }
    }
}

fun main(args: Array<String>) {
    application(BackgroundRemovalMoving(),
            configuration {
                width = 1280
                height = 720
            })
}