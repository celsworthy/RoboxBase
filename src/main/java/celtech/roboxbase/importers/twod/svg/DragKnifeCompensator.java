package celtech.roboxbase.importers.twod.svg;

import celtech.roboxbase.importers.twod.svg.metadata.dragknife.DragKnifeMetaCut;
import celtech.roboxbase.importers.twod.svg.metadata.dragknife.DragKnifeMetaLift;
import celtech.roboxbase.importers.twod.svg.metadata.dragknife.StylusMetaPart;
import celtech.roboxbase.importers.twod.svg.metadata.dragknife.DragKnifeMetaPlunge;
import celtech.roboxbase.importers.twod.svg.metadata.dragknife.DragKnifeMetaTravel;
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

    public List<StylusMetaPart> doCompensation(List<StylusMetaPart> uncompensatedParts, double forwards_value)
    {
        boolean toolDown = false;

        List<StylusMetaPart> compensatedParts = new ArrayList();

        StylusMetaPart lastPartUnderExamination = null;

        for (StylusMetaPart uncompensatedPart : uncompensatedParts)
        {
            if (uncompensatedPart.getStart().equals(uncompensatedPart.getEnd()))
            {
                steno.warning("Discarding meta part:" + uncompensatedPart.renderToGCode());
            } else
            {

                StylusMetaPart compensatedPart = null;
                Vector2D newSpan;

                if (uncompensatedPart instanceof DragKnifeMetaTravel)
                {
                    if (toolDown)
                    {
                        compensatedParts.add(new DragKnifeMetaLift());
                        toolDown = false;
                    }

                    // Leave the travel alone
                    compensatedPart = uncompensatedPart;
                } else if (uncompensatedPart instanceof DragKnifeMetaCut)
                {
                    //Shift along vector
                    Vector2D vectorForThisSegment = uncompensatedPart.getEnd().subtract(uncompensatedPart.getStart());
                    Vector2D resultant_norm = vectorForThisSegment.normalize();
                    Vector2D shiftVector = resultant_norm.scalarMultiply(forwards_value);
                    try
                    {
                        compensatedPart = uncompensatedPart.getClass().newInstance();
                        compensatedPart.setStart(uncompensatedPart.getStart().add(shiftVector));
                        compensatedPart.setEnd(uncompensatedPart.getEnd().add(shiftVector));
                        compensatedPart.setComment(uncompensatedPart.getComment().concat(" - shifted"));

                        if (lastPartUnderExamination != null
                                && lastPartUnderExamination instanceof DragKnifeMetaCut)
                        {
                            //Chop the front of this segment off as there will be an arc towards it
                            Vector2D doubleShiftVector = resultant_norm.scalarMultiply(forwards_value * 2);
                            compensatedPart.setStart(uncompensatedPart.getStart().add(doubleShiftVector));

                            // We need to make an arc from the end of the last line to the start of this one
                            // The radius will be the offset and the arc centre will be the start of the uncompensated line
                            Vector2D vectorForLastSegment = lastPartUnderExamination.getEnd().subtract(lastPartUnderExamination.getStart());

                            double thisSegmentAngle = Math.atan2(vectorForThisSegment.getY(), vectorForThisSegment.getX());
                            double lastSegmentAngle = Math.atan2(vectorForLastSegment.getY(), vectorForLastSegment.getX());

                            steno.info("This segment angle: " + thisSegmentAngle);
                            compensatedPart.setComment(compensatedPart.getComment().concat(" Angle:" + thisSegmentAngle));
//                        steno.info("Last segment angle: " + lastSegmentAngle);

                            Vector2D arcCentre = uncompensatedPart.getStart();
                            Vector2D arcStart = compensatedPart.getStart();

                            double targetAngle = thisSegmentAngle;
                            double currentAngle = lastSegmentAngle;
                            if (lastSegmentAngle >= Math.PI && targetAngle < 0)
                            {
                                currentAngle = -lastSegmentAngle;
                            }

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
                                    compensatedParts.add(new DragKnifeMetaCut(fromX, fromY, newX, newY, "Swivel"));
                                    fromX = newX;
                                    fromY = newY;
                                }
                            }

//                        compensatedParts.add(new DragKnifeMetaCut(lastPartUnderExamination.getEnd().getX(),
//                                lastPartUnderExamination.getEnd().getY(),
//                                compensatedPart.getStart().getX(),
//                                compensatedPart.getStart().getY(),
//                                "Swivel"));
                        }
                    } catch (IllegalAccessException | InstantiationException ex)
                    {
                        steno.exception("Unable to instantiate part", ex);
                    }

                    if (!toolDown)
                    {
                        compensatedParts.add(new DragKnifeMetaPlunge());

                        double fromXTravel = 0;
                        double fromYTravel = 0;
                        if (lastPartUnderExamination != null)
                        {
                            fromXTravel = lastPartUnderExamination.getEnd().getX();
                            fromYTravel = lastPartUnderExamination.getEnd().getY();
                        }

                        compensatedParts.add(new DragKnifeMetaTravel(fromXTravel,
                                fromYTravel,
                                compensatedPart.getStart().getX(),
                                compensatedPart.getStart().getY(),
                                "Travel to start of cut"));

                        toolDown = true;
                    }
                } else
                {
                    steno.warning("Discarding part: " + uncompensatedPart.renderToGCode());
                }

                if (compensatedPart != null)
                {
                    compensatedParts.add(compensatedPart);
                }

                lastPartUnderExamination = uncompensatedPart;
            }
        }

        if (toolDown)
        {
            compensatedParts.add(new DragKnifeMetaLift());
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
}
