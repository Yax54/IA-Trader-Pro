package com.privateinvest.aitraderpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }  // R-09 : toggle
    var errorMessage by remember { mutableStateOf<String?>(null) }  // R-09 : validation

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Branding
        Text(
            "AI Trader Pro",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = PremiumBlue
        )
        Text(
            "Application privée d'investissement assisté",
            color = Color.LightGray,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 32.dp)
        )

        // Champ email
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            singleLine = true,
            isError = errorMessage != null && email.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // R-09 : champ mot de passe avec toggle visibilité
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Mot de passe") },
            singleLine = true,
            isError = errorMessage != null && password.isBlank(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Voir le mot de passe"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // R-09 : message d'erreur de validation
        errorMessage?.let { err ->
            Text(
                text = err,
                color = DangerRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // R-09 : validation avant d'appeler onLogin
        PremiumActionButton(
            text = "Accéder à l'application",
            onClick = {
                when {
                    email.isBlank() && password.isBlank() ->
                        errorMessage = "Veuillez saisir votre email et votre mot de passe."
                    email.isBlank() ->
                        errorMessage = "L'email est requis."
                    password.isBlank() ->
                        errorMessage = "Le mot de passe est requis."
                    else -> onLogin()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Application en mode simulation. Aucune décision automatique.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
