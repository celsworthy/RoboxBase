package celtech.roboxbase.importers.twod.svg;

import celtech.roboxbase.importers.twod.svg.metadata.dragknife.DragKnifeMetaLift;
import celtech.roboxbase.postprocessor.nouveau.nodes.GCodeEventNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.StylusLiftNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.StylusPlungeNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.StylusScribeNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.TravelNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.Movement;
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
                        double angleBetweenSegments = Vector2D.angle(vectorForThisSegment, lastVector);
                        if (angleBetweenSegments > 0.5)
                        {
                            // We need to make an arc from the end of the last line to the start of this one
                            // The radius will be the offset and the arc centre will be the start of the uncompensated line
                            double thisSegmentAngle = Math.atan2(vectorForThisSegment.getY(), vectorForThisSegment.getX());
                            double lastSegmentAngle = Math.atan2(lastVector.getY(), lastVector.getX());

                            compensatedPart.appendCommentText(" Angle:" + thisSegmentAngle);

                            Vector2D arcCentre = ((MovementProvider) lastUncompensatedPart).getMovement().toVector2D();

                            double targetAngle = thisSegmentAngle;
                            double currentAngle = lastSegmentAngle;

                            steno.info("Current angle: " + currentAngle + " target angle:" + targetAngle);
                            if (currentAngle >= Math.PI && targetAngle < 0)
                            {
                                currentAngle = -currentAngle;
                            } else if (targetAngle >= Math.PI && currentAngle < 0)
                            {
                                targetAngle = -targetAngle;
                            }

                            if (Math.abs(currentAngle - targetAngle) > 0.3)
                            {
                                double stepValue = Math.PI / 18;

                                if (currentAngle > targetAngle)
                                {
                                    stepValue = -stepValue;
                                }

                                while (Math.abs(currentAngle - targetAngle) >= Math.abs(stepValue))
                                {
                                    currentAngle += stepValue;
                                    double newX = arcCentre.getX() + Math.cos(currentAngle) * forwards_value;
                                    double newY = arcCentre.getY() + Math.sin(currentAngle) * forwards_value;
                                    StylusScribeNode swivelCut = new StylusScribeNode();
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
                        lastCompensatedPart.appendCommentText(" - shifted");
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

    private List<GCodeEventNode> addZMoves(List<GCodeEventNode> parts)
    {
        boolean toolDown = false;
        List<GCodeEventNode> partsWithZMoves = new ArrayList<>();

        for (GCodeEventNode part : parts)
        {
            if (part instanceof StylusScribeNode && !toolDown)
            {
                partsWithZMoves.add(new StylusPlungeNode(SVGConverterConfiguration.getInstance().getContactHeight()));
                toolDown = true;
            } else if (part instanceof TravelNode && toolDown)
            {
                partsWithZMoves.add(new StylusLiftNode(SVGConverterConfiguration.getInstance().getTravelHeight()));
                toolDown = false;
            }

            partsWithZMoves.add(part);
        }

        return partsWithZMoves;
    }
}
