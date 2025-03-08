package com.example.aesencryption

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.FileInputStream
import java.nio.charset.Charset
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val REQUEST_CODE_PICK_FILE = 1001

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request storage permissions
        requestStoragePermissions(this)

        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                AESEncryptionApp()
            } #manikanta
        }

    }

    private fun requestStoragePermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }
    }
}

@Composable
fun AESEncryptionApp() {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var filename by remember { mutableStateOf("encrypted_data.txt") } // Default filename
    var encryptedText by remember { mutableStateOf("") }
    var decryptedText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Encrypt") }
    var statusMessage by remember { mutableStateOf("") } // ✅ Separate variable for success


    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val contentResolver = context.contentResolver
                try {
                    contentResolver.openInputStream(it)?.use { inputStream ->
                        val content = inputStream.bufferedReader().readText()
                        val lines = content.split("\n")
                        val encryptedText = lines[0].replace("Encrypted Data: ", "")
                        val keyBytes = Base64.decode(lines[1].replace("Secret Key: ", ""), Base64.NO_WRAP)
                        val secretKey = SecretKeySpec(keyBytes, "AES")

                        decryptedText = decryptAES(encryptedText, secretKey)
                        error = "Decryption Successful"
                    }
                } catch (e: Exception) {
                    error = "Decryption Error: ${e.message}"
                    Log.e("AES_ERROR", "Decryption failed", e)
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AES Encryption and Decryption",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground // Dynamic text color
        )

        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Enter Text", color = MaterialTheme.colorScheme.onBackground) },
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )



        if (mode == "Encrypt") {
            TextField(
                value = filename,
                onValueChange = { filename = it },
                label = { Text("Enter Filename") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    mode = "Encrypt"
                    text = "" // Clear input text
                    decryptedText = "" // Clear decrypted text
                    encryptedText = "" // Clear encrypted text
                    error = "" // Clear error messages
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Encrypt Mode")
            }
            Button(
                onClick = {
                    mode = "Decrypt"
                    text = "" // Clear input text
                    decryptedText = "" // Clear decrypted text
                    encryptedText = "" // Clear encrypted text
                    error = "" // Clear error messages
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Decrypt Mode")
            }
        }


        if (mode == "Encrypt") {
            Button(
                onClick = {
                    encryptAndSave(
                        context = context,
                        text = text,
                        filename = filename, // Pass dynamic filename
                        onSuccess = { message -> error = message },
                        onError = { message -> error = message }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Encrypt and Save to File")
            }

        } else {
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("text/plain")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select File and Decrypt")
            }
        }

        if (encryptedText.isNotEmpty()) {
            Text("Encrypted Text: $encryptedText")
        }

        if (decryptedText.isNotEmpty()) {
            if (decryptedText.isNotEmpty()) {
                Text(
                    text = "Decrypted Text: $decryptedText",
                    color = MaterialTheme.colorScheme.onBackground, // Ensure proper contrast
                    fontSize = 16.sp
                )
            }

        }

        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = androidx.compose.ui.graphics.Color.Blue
            )
        }

        if (statusMessage.isNotEmpty()) {
            Text(
                text = "Success: $statusMessage",
                color = androidx.compose.ui.graphics.Color.Blue
            )
        }

    }
}



// Function to handle encryption and file saving
fun encryptAndSave(context: Context, text: String, filename: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
    try {
        val secretKey = generateRandomKey()
        val encryptedText = encryptAES(text, secretKey)
        saveToFile(context, filename, encryptedText, secretKey) // Pass filename
        onSuccess("File saved as $filename in Downloads folder")
    } catch (e: Exception) {
        Log.e("AES_ERROR", "Encryption failed", e)
        onError("Encryption Error: ${e.message}")
    }
}


// Generate a random AES Key
fun generateRandomKey(): SecretKeySpec {
    val keyBytes = ByteArray(32) // AES-256 bit key
    SecureRandom().nextBytes(keyBytes)
    return SecretKeySpec(keyBytes, "AES")
}

// Secure AES Encryption using CBC Mode
fun encryptAES(text: String, key: SecretKeySpec): String {
    val iv = ByteArray(16)
    SecureRandom().nextBytes(iv)
    val ivSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
    val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))

    val encryptedData = iv + encryptedBytes
    return Base64.encodeToString(encryptedData, Base64.NO_WRAP)
}

// Secure AES Decryption using CBC Mode
fun decryptAES(encryptedText: String, key: SecretKeySpec): String {
    val encryptedData = Base64.decode(encryptedText, Base64.NO_WRAP)

    val iv = encryptedData.copyOfRange(0, 16)
    val encryptedBytes = encryptedData.copyOfRange(16, encryptedData.size)

    val ivSpec = IvParameterSpec(iv)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
    val decryptedBytes = cipher.doFinal(encryptedBytes)

    return String(decryptedBytes, Charsets.UTF_8)
}

// Save encrypted text and key to a file in the Downloads folder
fun saveToFile(context: Context, filename: String, encryptedText: String, key: SecretKeySpec) {
    val keyString = Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    val content = "Encrypted Data: $encryptedText\nSecret Key: $keyString"

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Scoped Storage (Android 10+)
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename) // Use dynamic filename
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                    Log.d("FILE_DEBUG", "File successfully saved as $filename in Downloads: $uri")
                }
            } ?: Log.e("FILE_ERROR", "Failed to create file URI")
        } else {
            // Legacy file access (Android 9 and below)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)

            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray())
                Log.d("FILE_DEBUG", "File successfully saved as ${file.absolutePath}")
            }
        }
    } catch (e: IOException) {
        Log.e("FILE_ERROR", "Failed to save file", e)
    }
}



// Read encrypted text and key from file in the Downloads folder
fun readFromFile(): Pair<String, SecretKeySpec> {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloadsDir, "encrypted_data.txt")
    val content = FileInputStream(file).use { it.readBytes().toString(Charset.defaultCharset()) }
    val lines = content.split("\n")
    val encryptedText = lines[0].replace("Encrypted Data: ", "")
    val keyBytes = Base64.decode(lines[1].replace("Secret Key: ", ""), Base64.NO_WRAP)
    val secretKey = SecretKeySpec(keyBytes, "AES")
    return Pair(encryptedText, secretKey)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AESEncryptionApp()
}