import android.util.Log
import com.example.beaconkotlinapp.models.BeaconEvent
import com.example.beaconkotlinapp.utils.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object BeaconRepository {

    fun saveBeaconEvent(mac: String, lat: Double?, lon: Double?, phone: String, eventType: String) {
        val event = BeaconEvent(
            mac_address = mac,
            latitude = lat,
            longitude = lon,
            phone_number = phone,
            event_type = eventType
        )

        RetrofitClient.instance.sendBeaconEvent(event).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("API", "✅ Evento enviado correctamente")
                } else {
                    Log.e("API", "❌ Error al enviar evento: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("API", "❌ Error de red al enviar evento: ${t.message}")
            }
        })
    }
}
