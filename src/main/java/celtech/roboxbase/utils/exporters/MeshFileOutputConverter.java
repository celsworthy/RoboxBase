package celtech.roboxbase.utils.exporters;

import celtech.roboxbase.utils.threed.MeshToWorldTransformer;
import java.util.List;
import java.util.Map;
import javafx.scene.shape.MeshView;

/**
 *
 * @author Ian
 */
public interface MeshFileOutputConverter
{

    /**
     * Output the stl or amf file for the given project to the file indicated by the project job
     * UUID.
     * @param meshMap
     * @param printJobUUID
     * @param outputAsSingleFile
     * @return List of filenames that have been created
     */
    MeshExportResult outputFile(Map<MeshToWorldTransformer, List<MeshView>> meshMap, String printJobUUID, boolean outputAsSingleFile);

    /**
     * Output the stl or amf file for the given project to the file indicated by the project job
     * UUID.
     * @param meshMap
     * @param printJobUUID
     * @param printJobDirectory
     * @param outputAsSingleFile
     * @return List of filenames that have been created
     */
    MeshExportResult outputFile(Map<MeshToWorldTransformer, List<MeshView>> meshMap, String printJobUUID, String printJobDirectory, boolean outputAsSingleFile);
}
