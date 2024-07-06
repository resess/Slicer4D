package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.util.Statement


class JavaSlicerTest : SlicerTest() {
    fun testBasic() {
        runTest(
            "TestProjectBasic.jar", Statement("Main", 11),
            "20:1-21:1-3:1-4:1-7:1-8:1-11:1-5:1-6:1-",
            "Main:10\nMain:16\nMain:18\nMain:23\nMain:11",
            "462f343119270269af4a6e44c4745f69e7f0a75a2c3c25dbb6e3989922328a4f"
        )
    }

    fun testTiny() {
        runTest(
            "TestProjectTiny.jar", Statement("Main", 4),
            "3:1-6:1-7:1-4:1-5:1-",
            "Main:3\nMain:7\nMain:8\nMain:4",
            "7883b7b0979c3865b2e66af0e62c3bb6e12c656f88d67fcced5a52d46b012a17"
        )
    }

    fun testException() {
        runTest(
            "TestProjectException.jar", Statement("Main", 7),
            "3:1-8:1-4:1-14:1-16:1-9:1-10:1-12:1-19:1-20:1-7:1-6:1-",
            "Main:3\nMain:12\nMain:5\nMain:25\nMain:28\nMain:16\nMain:17\nMain:31\nMain:7",
            "cfd7c3e696ecfe9d696a119f1d1c46225b6877c54e0370063dddb593dc0da214"
        )
    }

    fun testStaticVariable() {
        runTest(
            "TestProjectStaticVariable.jar", Statement("Main", 7),
            "16:1-17:1-3:1-9:1-10:1-4:1-11:1-12:1-5:1-13:1-14:1-6:1-7:1-8:1-",
            "Main:5\nMain:16\nMain:6\nMain:20\nMain:21\nMain:7",
            "4bc8747f1f2ebe5eff6a1612cf691131c2fa9e56c609324f2db06e135395496b"
        )
    }

    fun testMultithreading() {
        runTest(
            "TestProjectMultithreading.jar", Statement("Main", 36),
            null, // Nondeterministic
            "Main:8\nMain\$lambda_main_0__1:-1\nMain:16\nMain:35\nMain\$lambda_main_1__2:-1\nMain:17\nMain:27\nMain:28\nMain:36",
            null, // Nondeterministic
        )
    }

    fun testMultipleClasses() {
        runTest(
            "TestProjectMultipleClasses.jar", Statement("Main2", 7),
            null, // Nondeterministic
            "Main2:18\nMain2:6\nMain2:7\nMain:8\nMain:7\nMain2:15\nMain2:-1",
            "14b58959344c43b0883aaa01b45bce71f4f69a7b87b3bcaef458572632af1fa8"
        )
    }
}
