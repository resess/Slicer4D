package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.util.SourceLocation

class ProgramSliceTest : SlicerTest() {
    fun testDependencies() {
        val dynamicSlice = runTest(
            "TestProjectTiny.jar", SourceLocation("Main", 4),
            null, null, null
        )
        val programSlice = ProgramSlice(null, dynamicSlice)
        val actualDependencies = programSlice.dependencies

        val line3 = SourceLocation("Main", 3)
        val line4 = SourceLocation("Main", 4)
        val line7 = SourceLocation("Main", 7)
        val line8 = SourceLocation("Main", 8)

        val expectedDependencies = mapOf(
            Pair(
                line3, ProgramSlice.Dependencies(
                    control = ProgramSlice.ControlDependencies(
                        to = listOf(ProgramSlice.ControlDependency(line7))
                    )
                )
            ),
            Pair(
                line7, ProgramSlice.Dependencies(
                    control = ProgramSlice.ControlDependencies(
                        from = listOf(ProgramSlice.ControlDependency(line3)),
                        to = listOf(ProgramSlice.ControlDependency(line8))
                    )
                )
            ),
            Pair(
                line4, ProgramSlice.Dependencies(
                    data = ProgramSlice.DataDependencies(
                        from = listOf(ProgramSlice.DataDependency(line8, "stack2"))
                    )
                )
            ),
            Pair(
                line8, ProgramSlice.Dependencies(
                    data = ProgramSlice.DataDependencies(
                        to = listOf(ProgramSlice.DataDependency(line4, "stack2"))
                    ),
                    control = ProgramSlice.ControlDependencies(
                        from = listOf(ProgramSlice.ControlDependency(line7))
                    )
                )
            )
        )

        assertEquals(expectedDependencies, actualDependencies)
    }

    fun testFirstLine() {
        val dynamicSlice = runTest(
            "TestProjectTiny.jar", SourceLocation("Main", 4),
            null, null, null
        )
        val programSlice = ProgramSlice(null, dynamicSlice)
        val actualFirstLine = programSlice.firstLine
        val expectedFirstLine = SourceLocation("Main", 2)
        assertEquals(expectedFirstLine, actualFirstLine)
    }
}