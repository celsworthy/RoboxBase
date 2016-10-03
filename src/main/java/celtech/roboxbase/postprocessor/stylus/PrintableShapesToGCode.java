package celtech.roboxbase.postprocessor.stylus;

import celtech.roboxbase.importers.twod.svg.PathParserThing;
import celtech.roboxbase.importers.twod.svg.SVGConverterConfiguration;
import celtech.roboxbase.importers.twod.svg.metadata.SVGMetaPart;
import celtech.roboxbase.importers.twod.svg.metadata.dragknife.PathHelper;
import celtech.roboxbase.postprocessor.nouveau.nodes.GCodeEventNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.TravelNode;
import celtech.roboxbase.utils.models.PrintableShapes;
import celtech.roboxbase.utils.models.ShapeForProcessing;
import celtech.roboxbase.utils.twod.ShapeToWorldTransformer;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.transform.BaseTransform;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.batik.parser.PathParser;

/**
 *
 * @author Ian
 */
public class PrintableShapesToGCode
{

    private static final Stenographer steno = StenographerFactory.getStenographer(PrintableShapesToGCode.class.getName());

    public static List<GCodeEventNode> parsePrintableShapes(PrintableShapes shapes)
    {
        List<SVGMetaPart> metaparts = new ArrayList<>();

        PathParserThing parserThing = new PathParserThing(metaparts);

        PathParser pathParser = new PathParser();
        pathParser.setPathHandler(parserThing);

//        for (ShapeForProcessing shapeForProcessing : shapes.getShapesForProcessing())
//        {
//            Shape shape = shapeForProcessing.getShape();
//            
//            if (shape instanceof SVGPath)
//            {
//                pathParser.parse(((SVGPath) shape).getContent());
//            } else if (shape instanceof Rectangle)
//            {
//                
//            } else
//            {
//                steno.warning("Unable to handle shape of type " + shape.getClass().getName());
//            }
//        }
//
//        List<StylusMetaPart> stylusMetaParts = SVGToStylusMetaEngine.convertToStylusMetaParts(metaparts);
//
//        StylusMetaToGCodeEngine stylusToGCode = new StylusMetaToGCodeEngine(BaseConfiguration.getPrintSpoolDirectory() + "stylusTest.gcode", stylusMetaParts);
//        return stylusToGCode.generateGCode();
        List<GCodeEventNode> gcodeEventNodes = new ArrayList<>();

        for (ShapeForProcessing shapeForProcessing : shapes.getShapesForProcessing())
        {
//            if (shapeForProcessing.getShape() instanceof SVGPath)
//            {
//                SVGPath pathToProcess = (SVGPath) shapeForProcessing.getShape();
//                final Path2D path2D = new Path2D(pathToProcess.impl_configShape());
////                final BaseTransform tx = pathToProcess.impl_getLeafTransform();
////                PathHelper pathHelper = new PathHelper(path2D, tx, 1.0);
////
////                PathIterator pathIterator = path2D.getPathIterator(tx);
//                int numberOfCommands = path2D.getNumCommands();
////
////                float[] points = new float[6];
////
////                while (!pathIterator.isDone())
////                {
////                    int elementType = pathIterator.currentSegment(points);
////                    
////                }
//
//            }
            gcodeEventNodes.addAll(renderShapeToGCode(shapeForProcessing));
        }

        return gcodeEventNodes;
    }

    private static List<GCodeEventNode> renderShapeToGCode(ShapeForProcessing shapeForProcessing)
    {
        List<GCodeEventNode> gcodeEvents = new ArrayList<>();

        Shape shapeToProcess = shapeForProcessing.getShape();
        ShapeToWorldTransformer shapeToWorldTransformer = shapeForProcessing.getShapeToWorldTransformer();

        if (shapeToProcess instanceof SVGPath)
        {
            SVGPath pathToProcess = (SVGPath) shapeForProcessing.getShape();
            final Path2D path2D = new Path2D(pathToProcess.impl_configShape());
            int numberOfCommands = path2D.getNumCommands();
            final BaseTransform tx = pathToProcess.impl_getLeafTransform();

            PathIterator pathIterator = path2D.getPathIterator(tx);
            float[] pathData = new float[6];
            Point2D currentPoint = null;

            while (!pathIterator.isDone())
            {
                int elementType = pathIterator.currentSegment(pathData);

                switch (elementType)
                {
                    case PathIterator.SEG_MOVETO:
                        currentPoint = shapeToWorldTransformer.transformShapeToRealWorldCoordinates(pathData[0], pathData[1]);
                        TravelNode travelToStart = new TravelNode();
                        travelToStart.setCommentText("Travel to start of path segment");
                        travelToStart.getFeedrate().setFeedRate_mmPerMin(SVGConverterConfiguration.getInstance().getTravelFeedrate());
                        travelToStart.getMovement().setX(currentPoint.getX());
                        travelToStart.getMovement().setY(currentPoint.getY());
                        gcodeEvents.add(travelToStart);
                        break;
                    case PathIterator.SEG_LINETO:
                        currentPoint = shapeToWorldTransformer.transformShapeToRealWorldCoordinates(pathData[0], pathData[1]);
                        TravelNode straightCut = new TravelNode();
                        straightCut.setCommentText("Straight cut");
                        straightCut.getFeedrate().setFeedRate_mmPerMin(SVGConverterConfiguration.getInstance().getCuttingFeedrate());
                        straightCut.getMovement().setX(currentPoint.getX());
                        straightCut.getMovement().setY(currentPoint.getY());
                        gcodeEvents.add(straightCut);
                        break;
                    case PathIterator.SEG_QUADTO:
                        QuadCurve newQuadCurve = new QuadCurve();
                        newQuadCurve.setStartX(currentPoint.getX());
                        newQuadCurve.setStartY(currentPoint.getY());
                        newQuadCurve.setControlX(pathData[0]);
                        newQuadCurve.setControlY(pathData[1]);
                        newQuadCurve.setEndX(pathData[2]);
                        newQuadCurve.setEndY(pathData[3]);
                        List<GCodeEventNode> quadCurveParts = renderCurveToGCodeNode(newQuadCurve, shapeToWorldTransformer);
                        gcodeEvents.addAll(quadCurveParts);
                        break;
                    case PathIterator.SEG_CUBICTO:
                        CubicCurve newCubicCurve = new CubicCurve();
                        newCubicCurve.setStartX(currentPoint.getX());
                        newCubicCurve.setStartY(currentPoint.getY());
                        newCubicCurve.setControlX1(pathData[0]);
                        newCubicCurve.setControlY1(pathData[1]);
                        newCubicCurve.setControlX2(pathData[2]);
                        newCubicCurve.setControlY2(pathData[3]);
                        newCubicCurve.setEndX(pathData[4]);
                        newCubicCurve.setEndY(pathData[5]);
                        List<GCodeEventNode> cubicCurveParts = renderCurveToGCodeNode(newCubicCurve, shapeToWorldTransformer);
                        gcodeEvents.addAll(cubicCurveParts);
                        break;
                    case PathIterator.SEG_CLOSE:
                        steno.info("Got a SEG_CLOSE");
                        break;
                }
                pathIterator.next();
            }

            List<GCodeEventNode> pathParts = renderCurveToGCodeNode(shapeToProcess, shapeToWorldTransformer, numberOfCommands * 50);
            gcodeEvents.addAll(pathParts);
        } else if (shapeToProcess instanceof Rectangle)
        {
            Bounds bounds = shapeToProcess.getBoundsInLocal();
            Point2D bottomLeft = shapeToWorldTransformer.transformShapeToRealWorldCoordinates((float) bounds.getMinX(), (float) bounds.getMinY());
            Point2D topRight = shapeToWorldTransformer.transformShapeToRealWorldCoordinates((float) bounds.getMaxX(), (float) bounds.getMaxY());

            TravelNode travelToStart = new TravelNode();
            travelToStart.setCommentText("Travel to start of Rectangle");
            travelToStart.getFeedrate().setFeedRate_mmPerMin(SVGConverterConfiguration.getInstance().getTravelFeedrate());
            travelToStart.getMovement().setX(bottomLeft.getX());
            travelToStart.getMovement().setY(bottomLeft.getY());

            TravelNode cut1 = new TravelNode();
            cut1.setCommentText("Cut 1");
            cut1.getFeedrate().setFeedRate_mmPerMin(SVGConverterConfiguration.getInstance().getCuttingFeedrate());
            cut1.getMovement().setX(bottomLeft.getX());
            cut1.getMovement().setY(topRight.getY());

            TravelNode cut2 = new TravelNode();
            cut2.setCommentText("Cut 2");
            cut2.getFeedrate().setFeedRate_mmPerMin(SVGConverterConfiguration.getInstance().getCuttingFeedrate());
            cut2.getMovement().setX(topRight.getX());
            cut2.getMovement().setY(topRight.getY());

            TravelNode cut3 = new TravelNode();
            cut3.setCommentText("Cut 3");
            cut3.getFeedrate().setFeedRate_mmPerMin(SVGConverterConfiguration.getInstance().getCuttingFeedrate());
            cut3.getMovement().setX(topRight.getX());
            cut3.getMovement().setY(bottomLeft.getY());

            TravelNode cut4 = new TravelNode();
            cut4.setCommentText("Cut 4");
            cut4.getFeedrate().setFeedRate_mmPerMin(SVGConverterConfiguration.getInstance().getCuttingFeedrate());
            cut4.getMovement().setX(bottomLeft.getX());
            cut4.getMovement().setY(bottomLeft.getY());

            gcodeEvents.add(travelToStart);
            gcodeEvents.add(cut1);
            gcodeEvents.add(cut2);
            gcodeEvents.add(cut3);
            gcodeEvents.add(cut4);
        } else if (shapeToProcess instanceof Circle
                || shapeToProcess instanceof Arc)
        {
            List<GCodeEventNode> circleParts = renderCurveToGCodeNode(shapeToProcess, shapeToWorldTransformer);
            gcodeEvents.addAll(circleParts);
        } else
        {
            steno.warning("Unable to handle shape of type " + shapeToProcess.getClass().getName());
        }

        return gcodeEvents;
    }

    private static List<GCodeEventNode> renderCurveToGCodeNode(Shape shape, ShapeToWorldTransformer shapeToWorldTransformer)
    {
        return renderCurveToGCodeNode(shape, shapeToWorldTransformer, 100);
    }

    private static List<GCodeEventNode> renderCurveToGCodeNode(Shape shape, ShapeToWorldTransformer shapeToWorldTransformer, int numberOfSegmentsToCreate)
    {
        List<GCodeEventNode> gcodeNodes = new ArrayList<>();

        final Path2D path2D = new Path2D(shape.impl_configShape());
        final BaseTransform tx = shape.impl_getLeafTransform();
        PathHelper pathHelper = new PathHelper(path2D, tx, 1.0);

        int numberOfSteps = numberOfSegmentsToCreate;
        for (int stepNum = 0; stepNum <= numberOfSteps; stepNum++)
        {
            double fraction = (double) stepNum / (double) numberOfSteps;
            Point2D position = pathHelper.getPosition2D(fraction, false);
            Point2D transformedPosition = shapeToWorldTransformer.transformShapeToRealWorldCoordinates((float) position.getX(), (float) position.getY());
            System.out.println("Input " + fraction + " X:" + position.getX() + " Y:" + position.getY());
            System.out.println("Transformed X:" + transformedPosition.getX() + " Y:" + transformedPosition.getY());

            TravelNode newTravel = new TravelNode();
            if (stepNum == 0)
            {
                newTravel.setCommentText("Move to start of curve");
            } else
            {
                newTravel.setCommentText("Curve cut");
            }
            newTravel.getMovement().setX(transformedPosition.getX());
            newTravel.getMovement().setY(transformedPosition.getY());
            newTravel.getFeedrate().setFeedRate_mmPerMin(SVGConverterConfiguration.getInstance().getCuttingFeedrate());
            gcodeNodes.add(newTravel);
        }

        return gcodeNodes;
    }
}
