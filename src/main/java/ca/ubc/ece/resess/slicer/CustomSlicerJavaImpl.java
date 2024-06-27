package ca.ubc.ece.resess.slicer;

import ca.ubc.ece.resess.ui.SelectSlicingCriterionAction;
import ca.ubc.ece.resess.util.SourceLocation;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CustomSlicerJavaImpl implements SlicerExtensionPoint {
    // Set the name which will be displayed in Available Slicers list
    @Override
    public @NotNull String getDisplayName() {
        return "Custom";
    }

    // Modify the below method to implement custom slicer
    @Override
    public SerializedProgramSlice createSlice(@NotNull Project project, RunnerAndConfigurationSettings settings, @NotNull DataContext dataContext, @NotNull Executor executor) {
        ExecutionEnvironmentBuilder builder = (settings == null) ? null :
                ExecutionEnvironmentBuilder.createOrNull(executor, settings);

        if (builder == null) {
            return null;
        }
        ExecutionEnvironment env = builder.activeTarget().dataContext(dataContext).build();
        UserDataHolder userDataHolder = (UserDataHolder) env.getDataContext();
        SourceLocation slicingCriteriaLocation = userDataHolder.getUserData(SelectSlicingCriterionAction.Companion.getSLICING_CRITERIA_KEY());

        // Complete the respective functions below, use the sourceLocation of the slicing criteria(slicingCriteriaLocation) if necessary
        Map<String, Set<Integer>> sliceLinesUnordered = createSliceLinesUnordered(slicingCriteriaLocation); // Map set of lineNumbers in slice to the respective file paths
        Map<SourceLocation, Dependencies> dependencies = createDependenciesMap(slicingCriteriaLocation); // Map the dependencies to respective sourceLocations
        SourceLocation firstLine = getFirstLine(slicingCriteriaLocation); // sourceLocation of the first line in slice
        return new SerializedProgramSlice(sliceLinesUnordered, dependencies, firstLine);
    }

    private Map<String, Set<Integer>> createSliceLinesUnordered(SourceLocation slicingCriteriaLocation) {
        Map<String, Set<Integer>> map = new HashMap<>();
        /*
        Add set of lineNumbers in the map with file path as key
        NOTE: Only in this map the line numbers should be 0 indexed, Line 1 in IDE means line 0 in the map, and so on
        */
        return map;
    }

    private Map<SourceLocation, Dependencies> createDependenciesMap(SourceLocation slicingCriteriaLocation) {
        Map<SourceLocation, Dependencies> map = new HashMap<>();
        /*
        Add Dependencies in the map.
        Refer end of template for definition of Dependencies class
        */
        return map;
    }

    private SourceLocation getFirstLine(SourceLocation slicingCriteriaLocation) {
        String clazz = "";
        int lineNumber = 0;
        /*
        Set the above class and line number appropriately
        Return the sourceLocation of the first line in slice.
        Refer end of template for definition of SourceLocation class
        */
        return new SourceLocation(clazz, lineNumber);
    }
}

/*

NOTE: The below code is implemented in Kotlin and has getters and setters by default.
example: dependency.getLocation() is a valid function call even though no method named getLocation is explicitly defined

data class SerializedProgramSlice(
    val sliceLinesUnordered: Map<String, Set<Int>>,
    val dependencies: Map<SourceLocation, Dependencies>,
    val firstLine: SourceLocation?,
)

class SourceLocation(val clazz: String, val lineNo: Int) {
    override fun toString() = "$clazz:$lineNo"

    override fun equals(other: Any?) = (other is SourceLocation)
            && clazz == other.clazz
            && lineNo == other.lineNo

    override fun hashCode() = Objects.hash(clazz, lineNo)
}

class Dependencies(
    val data: DataDependencies = DataDependencies(),
    val control: ControlDependencies = ControlDependencies()
) {
    override fun equals(other: Any?) = (other is Dependencies) && data == other.data && control == other.control
    override fun hashCode() = Objects.hash(data, control)
}

class DataDependencies(
    val from: List<DataDependency> = ArrayList(),
    val to: List<DataDependency> = ArrayList()
) {
    override fun equals(other: Any?) = (other is DataDependencies) && from == other.from && to == other.to
    override fun hashCode() = Objects.hash(from, to)
}

class ControlDependencies(
    val from: List<ControlDependency> = ArrayList(),
    val to: List<ControlDependency> = ArrayList()
) {
    override fun equals(other: Any?) = (other is ControlDependencies) && from == other.from && to == other.to
    override fun hashCode() = Objects.hash(from, to)
}

abstract class Dependency(val location: SourceLocation) {
    override fun equals(other: Any?) = (other is Dependency) && location == other.location
    override fun hashCode() = location.hashCode()
}

class ControlDependency(location: SourceLocation) : Dependency(location)

class DataDependency(location: SourceLocation, val variableName: String) : Dependency(location) {
    override fun equals(other: Any?) = (other is DataDependency)
            && location == other.location && variableName == other.variableName

    override fun hashCode() = Objects.hash(location, variableName)
}
*/