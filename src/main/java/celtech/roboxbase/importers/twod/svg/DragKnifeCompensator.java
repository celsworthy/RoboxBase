package celtech.roboxbase.importers.twod.svg;

import celtech.roboxbase.postprocessor.nouveau.nodes.GCodeEventNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.StylusLiftNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.StylusPlungeNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.StylusScribeNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.StylusSwivelNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.TravelNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.MovementProvider;
import java.util.ArrayList;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author ianhudson
 *
 */
public class DragKnifeCompensator
{

    private final Stenographer steno = StenographerFactory.getStenographer(DragKnifeCompensator.class.getName());

    public List<GCodeEventNode> doCompensation(List<GCodeEventNode> uncompensatedParts, double forwards_value)
    {
        List<GCodeEventNode> compensatedParts = new ArrayList();

        GCodeEventNode lastUncompensatedPart = null;
        GCodeEventNode lastCompensatedPart = null;
        Vector2D lastVector = null;

        for (GCodeEventNode uncompensatedPart : uncompensatedParts)
        {
            GCodeEventNode compensatedPart = null;

            if (uncompensatedPart instanceof TravelNode)
            {
                // Leave the travel alone
                compensatedPart = uncompensatedPart;

            } else if (uncompensatedPart instanceof StylusScribeNode)
            {
                StylusScribeNode uncompensatedStylusScribeNode = (StylusScribeNode) uncompensatedPart;

                //Shift along vector
                Vector2D vectorForThisSegment = uncompensatedStylusScribeNode.getMovement().toVector2D().subtract(((MovementProvider) lastUncompensatedPart).getMovement().toVector2D());
                double vectorMagnitude = Math.sqrt(Math.pow(vectorForThisSegment.getX(), 2.0) + Math.pow(vectorForThisSegment.getY(), 2.0));
                Vector2D resultant_norm = vectorForThisSegment.normalize();
                Vector2D shiftVector = resultant_norm.scalarMultiply(forwards_value);

                compensatedPart = new StylusScribeNode();
                Vector2D newEnd = uncompensatedStylusScribeNode.getMovement().toVector2D().add(shiftVector);
                ((MovementProvider) compensatedPart).getMovement().setX(newEnd.getX());
                ((MovementProvider) compensatedPart).getMovement().setY(newEnd.getY());
                compensatedPart.appendCommentText(" - shifted");

                if (lastUncompensatedPart != null)
                {
                    boolean shiftLastSegment = false;

                    if (lastUncompensatedPart instanceof StylusScribeNode)
                    {
                        if (vectorMagnitude > forwards_value)
                        {
                            // We need to make an arc from the end of the last line to the start of this one
                            // The radius will be the offset and the arc centre will be the start of the uncompensated line
                            double thisSegmentAngle = Math.atan2(vectorForThisSegment.getY(), vectorForThisSegment.getX());
                            double lastSegmentAngle = Math.atan2(lastVector.getY(), lastVector.getX());

                            compensatedPart.appendCommentText(" Angle:" + thisSegmentAngle);

                            Vector2D arcCentre = ((MovementProvider) lastUncompensatedPart).getMovement().toVector2D();

                            ShortestArc shortestArc = new ShortestArc(lastSegmentAngle, thisSegmentAngle);

                            if (Math.abs(shortestArc.getAngularDifference()) > 0.3)
                            {
                                double arcPointAngle = shortestArc.getCurrentAngle();
                                while (Math.abs(arcPointAngle - shortestArc.getTargetAngle()) >= Math.abs(shortestArc.getStepValue()))
                                {
                                    arcPointAngle += shortestArc.getStepValue();
                                    double newX = arcCentre.getX() + Math.cos(arcPointAngle) * forwards_value;
                                    double newY = arcCentre.getY() + Math.sin(arcPointAngle) * forwards_value;
                                    StylusSwivelNode swivelCut = new StylusSwivelNode();
                                    swivelCut.setCommentText("Swivel");
                                    swivelCut.getMovement().setX(newX);
                                    swivelCut.getMovement().setY(newY);
                                    compensatedParts.add(swivelCut);
                                }
                            }
                        } else
                        {
                            shiftLastSegment = true;
                        }
                    } else
                    {
                        shiftLastSegment = true;
                    }

                    if (shiftLastSegment)
                    {
                        Vector2D newPosition = ((MovementProvider) lastCompensatedPart).getMovement().toVector2D().add(shiftVector);
                        ((MovementProvider) lastCompensatedPart).getMovement().setX(newPosition.getX());
                        ((MovementProvider) lastCompensatedPart).getMovement().setY(newPosition.getY());
                        lastCompensatedPart.appendCommentText(" - moved last segment");
                    }
                }
                lastVector = vectorForThisSegment;
            }

            if (compensatedPart != null)
            {
                compensatedParts.add(compensatedPart);
            }

            lastCompensatedPart = compensatedPart;
            lastUncompensatedPart = uncompensatedPart;
        }

        return addZMoves(compensatedParts);
    }

    enum StylusPosition
    {

        UNKNOWN,
        TRAVEL,
        CUT,
        SWIVEL
    }

    private List<GCodeEventNode> addZMoves(List<GCodeEventNode> parts)
    {
        StylusPosition position = StylusPosition.UNKNOWN;

        List<GCodeEventNode> partsWithZMoves = new ArrayList<>();

        for (GCodeEventNode part : parts)
        {
            if (part instanceof TravelNode && position != StylusPosition.TRAVEL)
            {
                partsWithZMoves.add(new StylusLiftNode(SVGConverterConfiguration.getInstance().getTravelHeight()));
                position = StylusPosition.TRAVEL;
            } else if (part instanceof StylusScribeNode && position != StylusPosition.CUT)
            {
                partsWithZMoves.add(new StylusPlungeNode(SVGConverterConfiguration.getInstance().getContactHeight()));
                position = StylusPosition.CUT;
            } else if (part instanceof StylusSwivelNode && position != StylusPosition.SWIVEL)
            {
                partsWithZMoves.add(new StylusPlungeNode(SVGConverterConfiguration.getInstance().getSwivelHeight()));
                position = StylusPosition.SWIVEL;
            }

            partsWithZMoves.add(part);
        }

        return partsWithZMoves;
    }

    private double normaliseAngle(double angle)
    {
        double outputAngle = 0;
        //Make a +/- pi angle into a 0-2pi (clockwise) angle
        if (angle < 0)
        {
            outputAngle = angle + (Math.PI * 2);
        } else
        {
            outputAngle = angle;
        }

        return outputAngle;
    }
}
