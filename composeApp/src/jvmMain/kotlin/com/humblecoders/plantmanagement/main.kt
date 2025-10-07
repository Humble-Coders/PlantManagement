package com.humblecoders.plantmanagement

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.FirestoreClient
import com.humblecoders.plantmanagement.repositories.AuthRepository
import com.humblecoders.plantmanagement.repositories.EntityRepository
import com.humblecoders.plantmanagement.repositories.FirebaseAuthRestClient
import com.humblecoders.plantmanagement.ui.navigation.AppNavigation
import com.humblecoders.plantmanagement.viewmodels.AuthViewModel
import com.humblecoders.plantmanagement.viewmodels.EntityViewModel
import java.io.ByteArrayInputStream

fun main() = application {
    // Firebase configuration - Replace with your actual config
    val firebaseConfig = mapOf(
        "type" to "service_account",
        "project_id" to "plantmanagement-b1db8",
        "private_key_id" to "a64544f7c0817477a15a65bc85adb328a38765bf",
        // Ensure newlines are real (\n -> newline)
        "private_key" to (
            "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDFXD4myj8XWOxK\nARt//ghbSefBX4Ko1ViqjzFiZHyRTkmDFR/we5XgPmy/WFvMvbBuBgW/pXql6gdq\naP/18WrUY4DPruTPrP5m4PL8IxHWeAuudfn6MPicCwP2/Kxi1DjU8teHRXMwNx/Y\nX0a0rWE5RLpN/aC054Q91pgF2KZhdYWZnnHNznIx7pKXDAkES+ozIwUpvjE0/zlv\nd67ZBHG9l/jLe1Ln0lsk4AUxpOJ9+B69gdLgYEWqBXYazGwLVjxng/vt5w4/H9V5\n5r5UCmZuFN0pNzA/sPXQPn8f9gweQhpAO5JSbWP5JCNntoJBRfx2VQpuKOp30Qf9\n4dg2WADHAgMBAAECggEAE/1p1if6B/VbyYsfK/GhCw4LSUzRQSSK3IuwZtTiwRz6\n2SoUmBkMbzAjd6YNdkloS1yUzHqIV7HCmoaUFmcjeOuzKlX+mwJcqjLyqZMrxVtX\nY45TiYHYTIXZGvUwyLnSH8+RAQSJzEJpGVNdKAO/qztI24lq86fXfyPzNfRhtdEx\n9Di4T97cJ6JEMYKQnOpuX17kjswSAnZvXK/acESbFggNXHUKAOs9s7171/l+Dzx/\njAi0Y4w7kGLK2rsVKZ5HxqdS3mr4IH+YaJoKgjhAQ0iMQHr7UoYBCOU9ZXUcCIRH\n4xOvcPak64a1a8pl7FOTf641h+3x9NkbmDKDJikBsQKBgQDhboFzhNjb0SzhJhCZ\nEEEiyR1g+PBpnY8TEdIGrJrx6k0X/4wNclm/ATw5PDuOS37xZrLh/SISQBWSbo5i\ns69P2VL8g5WCzGgxfDNF9ZNdPUZqRHCbM2XHB1irwXdyVHrftJ8mKkoR8wDOZWFr\n9DL5tbRx2cRjB29SMqOq6NpexQKBgQDgH0k/DYXSmUcjOBtddjM5MOVTchwxAo6L\n56p5CpaRsgE9pLyAPB1DF2hEYc6Z+SScT6AbEab62joeiuyDLvKw0csgDU3SRogX\nrlCy5uGwvRPinicU39CPeSIN4poAir/it5eyjZAUnt9/Lk3sxrru4ZLloPxAXsEu\nm+FCLfAaGwKBgQCBUV4LQYsRVkYzhh2BYLSs2sfyp/tp6st3egKMd5mO6wR8nsjd\nsdpP2sqMXsQfVamlm29EemyAIaj+TlN5xW/tE1MqreUnmlCvCntzR3PYydzJ+ybJ\nsUtSSCGSrbHysQmnqLqfLyU8dLTisX+YaQaQ/q5bnTuuP0aZ1ZjT4y3ZrQKBgQCC\n96Vc1DEkXO+mFA5hskXCoOERvzfcJ+tWtBz5OIe+Qe/Zrt98bCSJZS9H7oPFlEKh\nmSItT0ZthoK6AGKvUgr96sqxQzT7sL2sYO/Fa4ftOMBbCCI8X4HHpXiR9p4ZvPre\nqHxcGb9dXW2rK0rcF1F7cJPf3hAqKXToIK0keCW7iwKBgA0m3LWYBDDAWkQO5rEL\nFfatseS1uSl8MuwGaWQgY3tndxb0IpNN7c5X8CAkzdev159yrgzRrxsavKhtz5Ef\npGNoyzLuyfjIOKUGnHruwY/xdVd8dyPK6RAVwFGTPiZhpP2vYhNtaQ2FBBxZ5NRc\n0asfhIhcsSmtCTaWo/4iBlB5\n-----END PRIVATE KEY-----\n"
        ).replace("\\n", "\n"),
        "client_email" to "firebase-adminsdk-fbsvc@plantmanagement-b1db8.iam.gserviceaccount.com",
        "client_id" to "115863602133641715163",
        "auth_uri" to "https://accounts.google.com/o/oauth2/auth",
        "token_uri" to "https://oauth2.googleapis.com/token",
        "auth_provider_x509_cert_url" to "https://www.googleapis.com/oauth2/v1/certs",
        "client_x509_cert_url" to "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-fbsvc%40plantmanagement-b1db8.iam.gserviceaccount.com"
    )

    // Firebase Web API Key - Get this from Firebase Console > Project Settings > General
    val firebaseApiKey = "AIzaSyAD4vhvoF1Kybe-fX0mief74FGHBc4dRRQ"

    // App ID for Firestore path
    val appId = "default-app-id" // Change this to your actual app ID

    // Initialize Firebase
    val firebaseOk = initializeFirebase(firebaseConfig)

    // Initialize Firebase services (guard if initialization failed)
    val auth = if (firebaseOk) FirebaseAuth.getInstance() else null
    val firestore = if (firebaseOk) FirestoreClient.getFirestore() else null
    val firestoreNonNull = firestore ?: FirestoreClient.getFirestore()
    val restClient = FirebaseAuthRestClient(firebaseApiKey)

    // Initialize Auth Repository and ViewModel
    val authRepository = AuthRepository(
        auth ?: FirebaseAuth.getInstance(),
        firestoreNonNull,
        restClient
    )
    val authViewModel = AuthViewModel(authRepository)

    // Wait for auth to be ready and get userId
    var entityViewModel: EntityViewModel? = null

    val windowState = rememberWindowState(width = 1400.dp, height = 900.dp)

    Window(
        onCloseRequest = {
            restClient.close()
            exitApplication()
        },
        title = "Plant Management System",
        state = windowState
    ) {
        // Initialize EntityViewModel when user is authenticated
        val currentUser = authViewModel.authState.currentUser

        if (currentUser != null && entityViewModel == null) {
            val entityRepository = EntityRepository(firestoreNonNull, currentUser.uid, appId)
            entityViewModel = EntityViewModel(entityRepository)
        }

        if (entityViewModel != null) {
            AppNavigation(
                authViewModel = authViewModel,
                entityViewModel = entityViewModel!!
            )
        } else {
            // Show auth screens until user is logged in
            AppNavigation(
                authViewModel = authViewModel,
                entityViewModel = EntityViewModel(EntityRepository(firestoreNonNull, "", appId)) // Temporary
            )
        }
    }
}

private fun initializeFirebase(config: Map<String, String>): Boolean {
    try {
        val clientEmail = config["client_email"] ?: error("client_email missing")
        val clientId = config["client_id"] ?: error("client_id missing")
        val privateKey = config["private_key"] ?: error("private_key missing")
        val privateKeyId = config["private_key_id"] ?: error("private_key_id missing")

        val credentials: GoogleCredentials = ServiceAccountCredentials.fromPkcs8(
            clientId,
            clientEmail,
            privateKey,
            privateKeyId,
            listOf(
                "https://www.googleapis.com/auth/cloud-platform",
                "https://www.googleapis.com/auth/datastore"
            )
        )

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId(config["project_id"])
            .build()

        FirebaseApp.initializeApp(options)

        println("Firebase initialized successfully")
        return true
    } catch (e: Exception) {
        println("Error initializing Firebase: ${e.message}")
        e.printStackTrace()
        return false
    }
}