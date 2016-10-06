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
 */
public class DragKnifeCompensator
{

    private final Stenographer steno = StenographerFactory.getStenographer(DragKnifeCompensator.class.getName());

    public List<GCodeEventNode> doCompensation(List<GCodeEventNode> uncompensatedParts, double forwards_value)
    {
        List<GCodeEventNode> compensatedParts = new ArrayList();

        GCodeEventNode lastPartUnderExamination = null;

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
                Vector2D vectorForThisSegment = uncompensatedStylusScribeNode.getMovement().toVector2D().subtract(uncompensatedStylusScribeNode.getPreviousMovement().get().toVector2D());
                Vector2D resultant_norm = vectorForThisSegment.normalize();
                Vector2D shiftVector = resultant_norm.scalarMultiply(forwards_value);

                compensatedPart = new StylusScribeNode();
//                    Vector2D newStart = uncompensatedTravelNode.getPreviousMovement().get().toVector2D().add(shiftVector);
//                    Movement newMovement = new Movement();
//                    newMovement.setX(newStart.getX());
//                    newMovement.setY(newStart.getY());
//                    compensatedPart.setPreviousMovement(uncompensatedTravelNode.getPreviousMovement().get());
                Vector2D newEnd = uncompensatedStylusScribeNode.getMovement().toVector2D().add(shiftVector);
                ((StylusScribeNode) compensatedPart).getMovement().setX(newEnd.getX());
                ((StylusScribeNode) compensatedPart).getMovement().setX(newEnd.getY());
                ((StylusScribeNode) compensatedPart).appendCommentText(" - shifted");

                if (lastPartUnderExamination != null
                        && lastPartUnderExamination instanceof StylusScribeNode)
                {
                    //Chop the front of this segment off as there will be an arc towards it
                    Vector2D doubleShiftVector = resultant_norm.scalarMultiply(forwards_value * 2);
                    Vector2D modifiedStart = uncompensatedStylusScribeNode.getPreviousMovement().get().toVector2D().add(doubleShiftVector);
                    ((StylusScribeNode) lastPartUnderExamination).getMovement().setX(modifiedStart.getX());
                    ((StylusScribeNode) lastPartUnderExamination).getMovement().setY(modifiedStart.getY());
                    Movement compensatedPartMovement = new Movement();
                    compensatedPartMovement.setX(modifiedStart.getX());
                    compensatedPartMovement.setY(modifiedStart.getY());
                    ((StylusScribeNode) compensatedPart).setPreviousMovement(compensatedPartMovement);

                    // We need to make an arc from the end of the last line to the start of this one
                    // The radius will be the offset and the arc centre will be the start of the uncompensated line
                    Vector2D vectorForLastSegment = ((StylusScribeNode) lastPartUnderExamination).getMovement().toVector2D().subtract(((StylusScribeNode) lastPartUnderExamination).getPreviousMovement().get().toVector2D());

                    double thisSegmentAngle = Math.atan2(vectorForThisSegment.getY(), vectorForThisSegment.getX());
                    double lastSegmentAngle = Math.atan2(vectorForLastSegment.getY(), vectorForLastSegment.getX());

                    steno.info("This segment angle: " + thisSegmentAngle);
                    compensatedPart.appendCommentText(" Angle:" + thisSegmentAngle);
//                        steno.info("Last segment angle: " + lastSegmentAngle);

                    Vector2D arcCentre = uncompensatedStylusScribeNode.getPreviousMovement().get().toVector2D();
                    Vector2D arcStart = ((StylusScribeNode) compensatedPart).getPreviousMovement().get().toVector2D();

                    double targetAngle = thisSegmentAngle;
                    double currentAngle = lastSegmentAngle;
                    if (lastSegmentAngle >= Math.PI && targetAngle < 0)
                    {
                        currentAngle = -lastSegmentAngle;
                    }

                    StylusScribeNode lastPartToModify = (StylusScribeNode) lastPartUnderExamination;

                    if (Math.abs(currentAngle - targetAngle) > 0.3)
                    {
                        double stepValue = Math.PI / 18;

                        if (currentAngle > targetAngle)
                        {
                            stepValue = -stepValue;
                        }

                        double fromX = arcStart.getX();
                        double fromY = arcStart.getY();

                        while (Math.abs(currentAngle - targetAngle) >= Math.abs(stepValue))
                        {
                            currentAngle += stepValue;
                            double newX = arcCentre.getX() + Math.cos(currentAngle) * forwards_value;
                            double newY = arcCentre.getY() + Math.sin(currentAngle) * forwards_value;
                            StylusScribeNode newTravel = new StylusScribeNode();
                            newTravel.setCommentText("Swivel");
                            Movement startMovement = new Movement();
                            startMovement.setX(fromX);
                            startMovement.setY(fromY);
                            lastPartToModify.getMovement().setX(fromX);
                            lastPartToModify.getMovement().setY(fromY);
                            newTravel.setPreviousMovement(startMovement);
                            newTravel.getMovement().setX(newX);
                            newTravel.getMovement().setY(newY);
                            compensatedParts.add(newTravel);
                            fromX = newX;
                            fromY = newY;
                            lastPartToModify = newTravel;
                        }
                    }
                }

                if (compensatedPart != null)
                {
                    compensatedParts.add(compensatedPart);
                }

                lastPartUnderExamination = uncompensatedStylusScribeNode;
            }
        }

//        List<DragKnifeMetaPart> finalPartsList = new ArrayList();
//
//        Vector2D lastSegment = null;
//        StylusMetaPart lastPartUnderExamination = null;
//
//        for (StylusMetaPart partUnderExamination : compensatedParts)
//        {
//            Vector2D thisSegment = partUnderExamination.getEnd().subtract(partUnderExamination.getStart());
//
//            if (lastPartUnderExamination != null)
//            {
//                double angle = Vector2D.angle(lastSegment, thisSegment);
//                System.out.println("Got angle of " + angle);
//                if (Math.abs(angle) > 0.5)
//                {
//                    //Continue the last segment by the offset
//                    try
//                    {
//                        StylusMetaPart moveContinuation = lastPartUnderExamination.getClass().newInstance();
//                        moveContinuation.setStart(lastPartUnderExamination.getEnd());
//                        moveContinuation.setEnd(lastPartUnderExamination.getEnd().add(forwards_value, thisSegment.normalize()));
//                        moveContinuation.setComment("Added continuation");
//                        finalPartsList.add(moveContinuation);
//                    } catch (InstantiationException | IllegalAccessException ex)
//                    {
//                        steno.exception("Failed to create continuation part", ex);
//                    }
//                }
//            }
//
//            finalPartsList.add(partUnderExamination);
//
//            lastSegment = thisSegment;
//            lastPartUnderExamination = partUnderExamination;
//        }
        return compensatedParts;
    }

    private List<GCodeEventNode> addZMoves(List<GCodeEventNode> parts)
    {
        List<GCodeEventNode> outputBuffer = null;
//        if (!toolDown)
//        {
//            compensatedParts.add(new StylusPlungeNode(SVGConverterConfiguration.getInstance().getContactHeight()));
//
//            double fromXTravel = 0;
//            double fromYTravel = 0;
//            if (lastPartUnderExamination != null)
//            {
//                fromXTravel = ((MovementProvider) lastPartUnderExamination).getMovement().getX();
//                fromYTravel = ((MovementProvider) lastPartUnderExamination).getMovement().getY();
//            }
//
//            TravelNode travelToStart = new TravelNode();
//            travelToStart.getMovement().setX(((MovementProvider) compensatedPart).getMovement().getX());
//            travelToStart.getMovement().setY(((MovementProvider) compensatedPart).getMovement().getY());
//            Movement travelToStartPriorMovement = new Movement();
//            travelToStartPriorMovement.setX(fromXTravel);
//            travelToStartPriorMovement.setY(fromYTravel);
//            travelToStart.setPreviousMovement(travelToStartPriorMovement);
//            travelToStart.setCommentText("Travel to start of cut");
//            compensatedParts.add(travelToStart);
//
//                    toolDown = true;
//                }

        return outputBuffer;
    }
}
