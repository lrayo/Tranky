import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Connection
import java.sql.PreparedStatement

object BeaconRepository {

    fun saveBeaconEvent(mac: String, lat: Double?, lon: Double?, phone: String, eventType: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val connection: Connection? = DatabaseManager.getConnection()
            if (connection != null) {
                val query = """
                    INSERT INTO beacon_events (mac_address, latitude, longitude, phone_number, event_type) 
                    VALUES (?, ?, ?, ?, ?)
                """

                try {
                    val statement: PreparedStatement = connection.prepareStatement(query)
                    statement.setString(1, mac)
                    statement.setObject(2, lat) // Puede ser null
                    statement.setObject(3, lon) // Puede ser null
                    statement.setString(4, phone)
                    statement.setString(5, eventType)

                    statement.executeUpdate()
                    println("✅ Evento del beacon guardado correctamente en PostgreSQL")

                    statement.close()
                    connection.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("❌ Error al guardar el evento del beacon en la base de datos")
                }
            }
        }
    }
}
