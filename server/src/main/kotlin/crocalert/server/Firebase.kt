package crocalert.server

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import java.io.FileInputStream

object FirebaseInit {

    fun init() {
        val serviceAccount = FirebaseInit::class.java.getResourceAsStream("/firebase/serviceAccountKey.json")
            ?: error("No se encontró /firebase/serviceAccountKey.json en resources")

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        FirebaseApp.initializeApp(options)
    }

    fun firestore() = FirestoreClient.getFirestore()
}