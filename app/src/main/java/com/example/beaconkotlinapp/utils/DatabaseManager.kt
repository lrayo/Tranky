import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DatabaseManager {
    private const val URL = "jdbc:postgresql://ep-rapid-brook-a4bdlztx-pooler.us-east-1.aws.neon.tech/neondb?sslmode=require"
    private const val USER = "neondb_owner"
    private const val PASSWORD = "npg_8LBHPVWg0zaK"

    fun getConnection(): Connection? {
        return try {
            DriverManager.getConnection(URL, USER, PASSWORD)
        } catch (e: SQLException) {
            e.printStackTrace()
            null
        }
    }
}
