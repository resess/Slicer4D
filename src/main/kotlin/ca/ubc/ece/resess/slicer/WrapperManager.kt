package ca.ubc.ece.resess.slicer

class WrapperManager {
    companion object {
        @JvmStatic
        var testWrapper: APILayer = Slicer4JWrapper()

        @JvmStatic
        private var currentWrapper: SlicerExtensionPoint = DefaultSlicerExtensionPointImpl()

        @JvmStatic
        fun getCurrentWrapper(): SlicerExtensionPoint {
            return currentWrapper
        }

        @JvmStatic
        fun setCurrentWrapper(wrapper: SlicerExtensionPoint) {
            currentWrapper = wrapper
        }
    }

//    companion object {
//        @JvmStatic
//        private var currentWrapper: APILayer = Slicer4JWrapper()
//
//        @JvmStatic
//        fun getCurrentWrapper(): APILayer {
//            return currentWrapper
//        }
//
//        @JvmStatic
//        fun setCurrentWrapper(wrapper: APILayer) {
//            currentWrapper = wrapper
//        }
//    }
}