package com.example.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.LocationUtils

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLength: Int = 200,
    supportingText: String? = null,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder) }) else null,
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        supportingText = supportingText?.let { { Text(it, fontSize = 11.sp) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

/**
 * Bouton « Utiliser ma position » : demande la permission de localisation approximative,
 * puis renvoie la dernière position connue. Optionnel : l'application fonctionne sans.
 */
@Composable
fun UseMyLocationButton(onLocation: (lat: Double, lng: Double) -> Unit, onUnavailable: () -> Unit = {}) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val loc = if (granted) LocationUtils.lastKnownLocation(context) else null
        if (loc != null) onLocation(loc.first, loc.second) else onUnavailable()
    }
    OutlinedButton(
        onClick = {
            if (LocationUtils.hasPermission(context)) {
                val loc = LocationUtils.lastKnownLocation(context)
                if (loc != null) onLocation(loc.first, loc.second) else onUnavailable()
            } else {
                launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Utiliser ma position")
    }
}

@Composable
fun FormColumn(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}

@Composable
fun FieldHint(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
}
