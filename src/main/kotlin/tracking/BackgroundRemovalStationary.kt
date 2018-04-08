package tracking

import org.openrndr.Program

import boofcv.struct.image.GrayF32
import boofcv.struct.image.ImageType
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector

import boofcv.alg.background.BackgroundModelStationary
import boofcv.factory.background.ConfigBackgroundBasic
import boofcv.factory.background.FactoryBackgroundModel
import boofcv.struct.image.GrayU8

import org.openrndr.application
import org.openrndr.configuration
import org.openrndr.draw.grayscale
import org.openrndr.ffmpeg.FFMPEGVideoPlayer
import org.openrndr.xboofcv.colorBufferFromGrayU8
import org.openrndr.xboofcv.grayF32fromColorBuffer


class BackgroundRemovalStationary : Program() {
    var fileName = "/users/edwin/git/openrndr-x-boofcv/data/background/street_intersection.mp4"
    var imageType = ImageType.single(GrayF32::class.java)
    lateinit var background: BackgroundModelStationary<GrayF32>


    lateinit var videoPlayer: FFMPEGVideoPlayer

    lateinit var grey: GrayF32
    lateinit var segmented: GrayU8

    override fun setup() {
        val confDetector = ConfigGeneralDetector()
        confDetector.threshold = 10f
        confDetector.maxFeatures = 300
        confDetector.radius = 6

        background = FactoryBackgroundModel.stationaryBasic<GrayF32>(ConfigBackgroundBasic(35f, 0.005f), imageType)
        videoPlayer = FFMPEGVideoPlayer.fromURL("file:$fileName")

        videoPlayer.start()
        videoPlayer.next()

        val w = videoPlayer.colorBuffer?.width?:-1
        val h = videoPlayer.colorBuffer?.height?:-1

        // storage for segmented image.  Background = 0, Foreground = 1
        segmented = GrayU8(w, h)
        // Grey scale image that's the input for motion estimation
        grey = GrayF32(segmented.width, segmented.height)
    }

    override fun draw() {

        videoPlayer.next()

        videoPlayer.colorBuffer?.let {
            val input = grayF32fromColorBuffer(it)
            drawer.image(it)
            drawer.translate(it.width.toDouble(), 0.0)
            background.updateBackground(input, segmented)
            val cb2 = colorBufferFromGrayU8(segmented)
            drawer.drawStyle.colorMatrix = grayscale(100.0, 100.0, 100.0)
            drawer.image(cb2)
            cb2.destroy()
        }
    }
}

fun main(args: Array<String>) {
    application(BackgroundRemovalStationary(),
            configuration {
                width = 1280
                height = 720
            })
}