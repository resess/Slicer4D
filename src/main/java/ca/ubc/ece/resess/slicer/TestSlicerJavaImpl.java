package ca.ubc.ece.resess.slicer;

import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class TestSlicerJavaImpl implements SlicerExtensionPoint {
    // Set the name which will be displayed in Available Slicers list
    @Override
    public @NotNull String getDisplayName() {
        return "JavaDummy";
    }

    // Modify the below method to implement custom slicer
    @Override
    public SerializedProgramSlice createSlice(@NotNull Project project, RunnerAndConfigurationSettings settings, @NotNull DataContext dataContext, @NotNull Executor executor) {
        // Complete the respective methods below
        DynamicSlice ds = new DynamicSlice();
        ProgramSlice ps = new ProgramSlice(null, ds, "C:\\Users\\Pranab\\IdeaProjects\\s4d_test\\ProgramSlice (2).json",null );
        return new SerializedProgramSlice(ps.getSliceLinesUnordered(), ps.getDependencies(), ps.getFirstLine());
    }
}
