//package geometry
//
//import org.openrndr.Program
//import org.openrndr.application
//import org.openrndr.configuration
//import boofcv.struct.geo.AssociatedPair
//import georegression.struct.point.Point2D_F64
//import boofcv.struct.feature.AssociatedIndex
//import java.util.ArrayList
//
//import boofcv.alg.descriptor.UtilFeature
//import georegression.struct.homography.Homography2D_F64
//import org.ddogleg.fitting.modelset.ModelMatcher
//import boofcv.abst.feature.associate.AssociateDescription
//import boofcv.abst.feature.detdesc.DetectDescribePoint
//import boofcv.struct.feature.TupleDesc
//import boofcv.struct.image.ImageGray
//import boofcv.factory.geo.ConfigRansac
//import boofcv.factory.geo.FactoryMultiViewRobust
//import boofcv.factory.feature.associate.FactoryAssociation
//import boofcv.struct.feature.BrightFeature
//import boofcv.abst.feature.associate.ScoreAssociation
//import boofcv.abst.feature.detect.interest.ConfigFastHessian
//import boofcv.factory.feature.detdesc.FactoryDetectDescribe
//import boofcv.io.image.ConvertBufferedImage
//import java.awt.image.BufferedImage
//import java.awt.RenderingHints
//import georegression.struct.point.Point2D_I32
//
//import boofcv.struct.image.GrayF32
//import boofcv.alg.distort.impl.DistortSupport
//import boofcv.struct.image.Planar
//import boofcv.alg.distort.ImageDistort
//import boofcv.core.image.border.BorderType
//import boofcv.factory.interpolate.FactoryInterpolation
//import boofcv.alg.interpolate.InterpolatePixelS
//import boofcv.alg.distort.PixelTransformHomography_F32
//import georegression.transform.homography.HomographyPointOps_F64
//import org.ddogleg.struct.FastQueue
//
//
//class ImageStitching : Program() {
//
//    fun <T : ImageGray<T>, FD : TupleDesc<*>> computeTransform(imageA: T, imageB: T,
//                                                               detDesc: DetectDescribePoint<T, FD>,
//                                                               associate: AssociateDescription<FD>,
//                                                               modelMatcher: ModelMatcher<Homography2D_F64, AssociatedPair>): Homography2D_F64 {
//        // get the length of the description
//        val pointsA = ArrayList<Point2D_F64>()
//        val descA = UtilFeature.createQueue(detDesc, 100)
//        val pointsB = ArrayList<Point2D_F64>()
//        val descB = UtilFeature.createQueue(detDesc, 100)
//
//        // extract feature locations and descriptions from each image
//        describeImage(imageA, detDesc, pointsA, descA)
//        describeImage(imageB, detDesc, pointsB, descB)
//
//        // Associate features between the two images
//        associate.setSource(descA)
//        associate.setDestination(descB)
//        associate.associate()
//
//        // create a list of AssociatedPairs that tell the model matcher how a feature moved
//        val matches = associate.matches
//        val pairs = ArrayList<AssociatedPair>()
//
//        for (i in 0 until matches.size()) {
//            val match = matches.get(i)
//
//            val a = pointsA[match.src]
//            val b = pointsB[match.dst]
//
//            pairs.add(AssociatedPair(a, b, false))
//        }
//
//        // find the best fit model to describe the change between these images
//        if (!modelMatcher.process(pairs))
//            throw RuntimeException("Model Matcher failed!")
//
//        // return the found image transform
//        return modelMatcher.modelParameters.copy()
//    }
//
//    /**
//     * Detects features inside the two images and computes descriptions at those points.
//     */
//    private fun <T : ImageGray<T>, FD : TupleDesc<*>> describeImage(image: T,
//                                                                     detDesc: DetectDescribePoint<T, FD>,
//                                                                     points: MutableList<Point2D_F64>,
//                                                                     listDescs: FastQueue<FD>) {
//        detDesc.detect(image)
//
//        listDescs.reset()
//        for (i in 0 until detDesc.numberOfFeatures) {
//            points.add(detDesc.getLocation(i).copy())
//            val item = detDesc.getDescription(i)
//            //listDescs.grow().setTo(item)
//            val b: FD = listDescs.grow()
//            b.setTo(item as Nothing)
//
//        }
//    }
//
//    /**
//     * Given two input images create and display an image where the two have been overlayed on top of each other.
//     */
//    fun <T : ImageGray<T>> stitch(imageA: BufferedImage, imageB: BufferedImage, imageType: Class<T>) {
//        val inputA = ConvertBufferedImage.convertFromSingle(imageA, null, imageType)
//        val inputB = ConvertBufferedImage.convertFromSingle(imageB, null, imageType)
//
//        // Detect using the standard SURF feature descriptor and describer
//        val detDesc = FactoryDetectDescribe.surfStable<T, GrayF32>(
//                ConfigFastHessian(1f, 2, 200, 1, 9, 4, 4), null, null, imageType)
//        val scorer = FactoryAssociation.scoreEuclidean(BrightFeature::class.java, true)
//        val associate = FactoryAssociation.greedy(scorer, 2.0, true)
//
//        // fit the images using a homography.  This works well for rotations and distant objects.
//        val modelMatcher = FactoryMultiViewRobust.homographyRansac(null, ConfigRansac(60, 3.0))
//
//        val H = computeTransform(inputA, inputB, detDesc, associate, modelMatcher)
//
//        renderStitching(imageA, imageB, H)
//    }
//
//
//    /**
//     * Renders and displays the stitched together images
//     */
//    fun renderStitching(imageA: BufferedImage, imageB: BufferedImage,
//                        fromAtoB: Homography2D_F64) {
//        // specify size of output image
//        val scale = 0.5
//
//        // Convert into a BoofCV color format
//        val colorA = ConvertBufferedImage.convertFromPlanar(imageA, null, true, GrayF32::class.java)
//        val colorB = ConvertBufferedImage.convertFromPlanar(imageB, null, true, GrayF32::class.java)
//
//        // Where the output images are rendered into
//        val work = colorA.createSameShape()
//
//        // Adjust the transform so that the whole image can appear inside of it
//        val fromAToWork = Homography2D_F64(scale, 0.0, (colorA.width / 4).toDouble(), 0.0, scale, (colorA.height / 4).toDouble(), 0.0, 0.0, 1.0)
//        val fromWorkToA = fromAToWork.invert(null)
//
//        // Used to render the results onto an image
//        val model = PixelTransformHomography_F32()
//        val interp = FactoryInterpolation.bilinearPixelS(GrayF32::class.java, BorderType.ZERO)
//        val distort = DistortSupport.createDistortPL(GrayF32::class.java, model, interp, false)
//        distort.renderAll = false
//
//        // Render first image
//        model.set(fromWorkToA)
//        distort.apply(colorA, work)
//
//        // Render second image
//        val fromWorkToB = fromWorkToA.concat(fromAtoB, null)
//        model.set(fromWorkToB)
//        distort.apply(colorB, work)
//
//        // Convert the rendered image into a BufferedImage
//        val output = BufferedImage(work.width, work.height, imageA.type)
//        ConvertBufferedImage.convertTo(work, output, true)
//
//        val g2 = output.createGraphics()
//
//        // draw lines around the distorted image to make it easier to see
//        val fromBtoWork = fromWorkToB.invert(null)
////        val corners = arrayOfNulls<Point2D_I32>(4)
////        corners[0] = renderPoint(0, 0, fromBtoWork)
////        corners[1] = renderPoint(colorB.width, 0, fromBtoWork)
////        corners[2] = renderPoint(colorB.width, colorB.height, fromBtoWork)
////        corners[3] = renderPoint(0, colorB.height, fromBtoWork)
////
////        g2.color = Color.ORANGE
////        g2.stroke = BasicStroke(4)
////        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
////        g2.drawLine(corners[0].x, corners[0].y, corners[1].x, corners[1].y)
////        g2.drawLine(corners[1].x, corners[1].y, corners[2].x, corners[2].y)
////        g2.drawLine(corners[2].x, corners[2].y, corners[3].x, corners[3].y)
////        g2.drawLine(corners[3].x, corners[3].y, corners[0].x, corners[0].y)
//
//        //ShowImages.showWindow(output, "Stitched Images", true)
//    }
//
//
//    fun renderPoint(x0: Int, y0: Int, fromBtoWork: Homography2D_F64): Point2D_I32 {
//        var result = Point2D_F64()
//        HomographyPointOps_F64.transform(fromBtoWork, Point2D_F64 (x0.toDouble(), y0.toDouble()), result);
//        return  Point2D_I32 ( result.x.toInt(), result.y.toInt())
//    }
//
//
//}
//
//
//fun main(args: Array<String>) {
//    application(ImageStitching(),
//            configuration {
//
//            })
//}