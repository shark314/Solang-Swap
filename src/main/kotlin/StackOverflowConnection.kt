import SoLangConfiguration.apiToken
import SoLangConfiguration.stackAuthenticationData
import SoLangConfiguration.stackConnectionParameters
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.coroutines.awaitString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object StackOverflowConnection {

    private const val stackExchangeApiUrl = "https://api.stackexchange.com/2.2/posts"

    init {
        configureFuel()
    }

    private val parameters: List<Pair<String, String>> = stackConnectionParameters
        get() = if (apiToken.isEmpty()) field else field + stackAuthenticationData

    private fun configureFuel() {
        FuelManager.instance.baseHeaders = mapOf("Content-Type" to "application/json")
        FuelManager.instance.basePath = stackExchangeApiUrl
    }

    internal val getAnswerBody = ::downloadAnswerBody.memSuspend()

    internal val getAnswerAllRevisionBodies = ::downloadAnswerAllRevisionBodies.memSuspend()

    private suspend fun downloadAnswerBody(platform: String, answerNumber: Int): String {
        val request = Fuel.get(answerNumber.toString(), parameters + ("site" to platform))
        val responseString = request.awaitString()
        val obj = Json.nonstrict.parse(ResponseModel.serializer(AnswerModel.serializer()), responseString)
        return obj.items.first().body
        // TODO when no longer experimental change it to Json.nonstrict.parse<ResponseModel<AnswerModel>>(responseString)
    }

    private suspend fun downloadAnswerAllRevisionBodies(platform: String, answerNumber: Int): Map<Int, String> {
        val request = Fuel.get("$answerNumber/revisions", parameters + ("site" to platform))
        val responseString = request.awaitString()
        val obj = Json.nonstrict.parse(ResponseModel.serializer(AnswerRevisionModel.serializer()), responseString)
        return mapOf(*obj.items.map { it.revision_number to it.body }.toTypedArray())
    }

    @Serializable
    private data class ResponseModel<T>(val items: List<T>)

    @Serializable
    private data class AnswerModel(val body: String)

    @Serializable
    private data class AnswerRevisionModel(val body: String, val revision_number: Int)
}
