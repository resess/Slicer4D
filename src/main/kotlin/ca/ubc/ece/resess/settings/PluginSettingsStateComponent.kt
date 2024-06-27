package ca.ubc.ece.resess.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.OptionTag
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json



@Service
@State(
    name = "pluginSettingsState",
    storages = arrayOf(Storage("settings.xml", roamingType = RoamingType.DISABLED)) // look for the xml file in build\idea-sandbox\config\options
)
class PluginSettingsStateComponent : PersistentStateComponent<PluginSettingsStateComponent> {
//    var slicingTechniques = listOf<String>("Traditional Slicing", "Dicing", "Barrier Slicing", "Thin Slicing")
    var sliceProviderFields = listOf<String>("name", "location")
    @OptionTag(converter = ListOfMapsOfStringsConverter::class)
    var sliceProviders = mutableListOf<HashMap<String, String>>()


    override fun getState(): PluginSettingsStateComponent? {
        return this
    }

    override fun loadState(state: PluginSettingsStateComponent) {
        XmlSerializerUtil.copyBean(state, this);
    }

    fun getSliceProviderCommand(index: Int): String?{
        return sliceProviders[index][sliceProviderFields[1]];
    }

}

//@Service
//@State(
//    name = "pluginSettingsState",
//    storages = arrayOf(Storage("settings.xml", roamingType = RoamingType.DISABLED)) // look for the xml file in build\idea-sandbox\config\options
//)
//class PluginSettingsStateComponent : SimplePersistentStateComponent<MyState>(MyState())
//
//class MyState : BaseState() {
//    var sliceProviderFields by list<String>()
//    var sliceProvider by list<Map<String, String>>()
//}


//@Serializable
//data class SliceProviderInformation(val name: String, val location: String, val url: String)

class ListOfMapsOfStringsConverter: Converter<List<HashMap<String, String>>>(){
    override fun toString(value: List<HashMap<String, String>>): String? {
        return Json.encodeToString(value)
    }

    override fun fromString(value: String): List<HashMap<String, String>>? {
        return Json.decodeFromString<List<HashMap<String, String>>>(value)
    }

}

