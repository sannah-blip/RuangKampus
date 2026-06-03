package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoginTab by remember { mutableStateOf(true) }

    // Input States
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("MAHASISWA") } // "MAHASISWA", "DOSEN", "ADMIN"
    var faculty by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }

    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    // Beautiful UI Palette mapping (matches sleek palette)
    val slateExtraDark = Color(0xFF0F172A)
    val sleekBlue = Color(0xFF2563EB)
    val sleekPurple = Color(0xFF9333EA)
    val cardBg = Color(0xFFFFFFFF)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                    } else {
                        listOf(Color(0xFFEFF6FF), Color(0xFFFAF5FF))
                    }
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Theme Toggle Icon
        IconButton(
            onClick = { viewModel.toggleTheme() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(
                    if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.7f),
                    RoundedCornerShape(12.dp)
                )
                .testTag("auth_theme_toggle")
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Ganti Tema",
                tint = if (isDarkTheme) Color.Yellow else Color(0xFF475569)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo Brand
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(sleekBlue, sleekPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = "Logo Sarpas",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "RuangKampus",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = slateExtraDark,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Portal Manajemen Sarana & Presensi Universitas",
                fontSize = 12.sp,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Sliding custom switch selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(true to "Masuk", false to "Daftar Akun").forEach { (isLogin, label) ->
                    val isSelected = isLoginTab == isLogin
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) sleekBlue else Color.Transparent)
                            .clickable { isLoginTab = isLogin }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Credential Entry Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isLoginTab) "Selamat Datang Kembali" else "Pendaftaran Anggota Baru",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = slateExtraDark,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Text(
                        text = if (isLoginTab) "Silakan masuk dengan akun yang sudah terdaftar." else "Buat akun baru Anda di SQLite lokal.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 2.dp, bottom = 18.dp)
                    )

                    // Register-only Field: Full Name
                    if (!isLoginTab) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Nama Lengkap") },
                            placeholder = { Text("cth: Andi Wicaksono") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("register_fullname_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = sleekBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }

                    // Username Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        placeholder = { Text("cth: mahasiswa") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("login_username_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = sleekBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (!isLoginTab) 12.dp else 24.dp)
                            .testTag("login_password_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = sleekBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    // Register-only Selection: Role & Faculty
                    if (!isLoginTab) {
                        Text(
                            text = "Tentukan Peran / Role Anda:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = slateExtraDark,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(vertical = 4.dp)
                        )

                        // Segmented Role Selection Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val roleOptions = listOf(
                                "MAHASISWA" to "Mahasiswa",
                                "DOSEN" to "Dosen",
                                "ADMIN" to "Admin"
                            )
                            roleOptions.forEach { (roleValue, label) ->
                                val isSelected = selectedRole == roleValue
                                val activeBg = if (roleValue == "ADMIN") sleekPurple else sleekBlue
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) activeBg else Color(0xFFF1F5F9))
                                        .clickable { selectedRole = roleValue }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // Faculty Input
                        OutlinedTextField(
                            value = faculty,
                            onValueChange = { faculty = it },
                            label = { Text("Fakultas / Unit Kerja") },
                            placeholder = { Text("cth: Fakultas Ilmu Komputer") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = sleekBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }

                    // Main Action Submit Button
                    Button(
                        onClick = {
                            if (isLoginTab) {
                                viewModel.loginUser(username, password) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel.registerUser(
                                    username = username,
                                    passwordPlain = password,
                                    fullName = fullName,
                                    role = selectedRole,
                                    faculty = faculty
                                ) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (success) {
                                        // Clear fields and toggle to login tab
                                        fullName = ""
                                        faculty = ""
                                        isLoginTab = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = sleekBlue
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isLoginTab) Icons.Default.Login else Icons.Default.PersonAdd,
                                contentDescription = null
                            )
                            Text(
                                text = if (isLoginTab) "Masuk Aplikasi" else "Daftar Sekarang",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Beautiful Quick-Access Default Accounts Card for Seamless Testing
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFDBEAFE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = sleekBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Petunjuk Akses (Akun Bawaan SQLite)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = slateExtraDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val quickUsers = listOf(
                        Triple("mahasiswa", "mhs123", "MAHASISWA"),
                        Triple("dosen", "dosen123", "DOSEN"),
                        Triple("admin", "admin123", "ADMIN")
                    )

                    quickUsers.forEach { (uq, pq, rq) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .clickable {
                                    username = uq
                                    password = pq
                                    Toast.makeText(context, "Menerapkan data login $rq", Toast.LENGTH_SHORT).show()
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "U: $uq  |  P: $pq",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = slateExtraDark
                                )
                                Text(
                                    text = "Klik untuk mengisi otomatis",
                                    fontSize = 9.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (rq == "ADMIN") sleekPurple.copy(alpha = 0.1f) 
                                        else sleekBlue.copy(alpha = 0.1f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = rq,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rq == "ADMIN") sleekPurple else sleekBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
