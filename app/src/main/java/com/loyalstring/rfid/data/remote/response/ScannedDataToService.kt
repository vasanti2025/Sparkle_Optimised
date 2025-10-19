import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ScannedDataToService(
    @SerializedName("Id")
    @Expose
    val id: Int? = null,

    @SerializedName("CreatedOn")
    @Expose
    val createdOn: String? = null,

    @SerializedName("LastUpdated")
    @Expose
    val lastUpdated: String? = null,

    @SerializedName("StatusType")
    @Expose
    val statusType: Boolean? = null,

    @SerializedName("ClientCode")
    @Expose
    val clientCode: String? = null,

    @SerializedName("DeviceId")
    @Expose
    val deviceId: String? = null,

    @SerializedName("TIDValue")
    @Expose
    val tIDValue: String? = null,

    @SerializedName("RFIDCode")
    @Expose
    val rFIDCode: String? = null
)
