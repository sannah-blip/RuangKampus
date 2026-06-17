import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/ruang_kampus_provider.dart';

class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final _formKey = GlobalKey<FormState>();
  bool _isLogin = true;

  // Form Fields
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  final _fullNameController = TextEditingController();
  String _selectedRole = "student";
  final _facultyController = TextEditingController(text: "Fakultas Ilmu Komputer");

  void _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final provider = Provider.of<RuangKampusProvider>(context, listen: false);

    if (_isLogin) {
      final success = await provider.login(
        _usernameController.text.trim(),
        _passwordController.text,
      );
      if (!mounted) return;
      if (!success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text("Username atau password salah!"),
            backgroundColor: Colors.redAccent,
          ),
        );
      }
    } else {
      final success = await provider.signUp(
        _usernameController.text.trim(),
        _passwordController.text,
        _fullNameController.text.trim(),
        _selectedRole,
        _facultyController.text.trim(),
      );
      if (!mounted) return;
      if (success) {
        setState(() => _isLogin = true);
        _usernameController.clear();
        _passwordController.clear();
        _fullNameController.clear();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<RuangKampusProvider>(context);
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: theme.colorScheme.surface, // Elegant dynamic background
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                provider.isDarkMode ? Icons.dark_mode : Icons.light_mode,
                size: 16,
                color: provider.isDarkMode ? const Color(0xFF818CF8) : const Color(0xFFD97706),
              ),
              const SizedBox(width: 6),
              // Hide label text on very small screens to prevent overflow
              LayoutBuilder(builder: (ctx, constraints) {
                return MediaQuery.of(ctx).size.width > 360
                    ? Text(
                        provider.isDarkMode ? "Tema Malam" : "Tema Siang",
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: theme.colorScheme.onSurface,
                        ),
                      )
                    : const SizedBox.shrink();
              }),
              const SizedBox(width: 4),
              Transform.scale(
                scale: 0.85,
                child: Switch(
                  value: provider.isDarkMode,
                  thumbColor: WidgetStateProperty.resolveWith((states) {
                    if (states.contains(WidgetState.selected)) return const Color(0xFF818CF8);
                    return const Color(0xFFD97706);
                  }),
                  trackColor: WidgetStateProperty.resolveWith((states) {
                    if (states.contains(WidgetState.selected)) return const Color(0xFF4F46E5).withValues(alpha: 0.4);
                    return Colors.black12;
                  }),
                  onChanged: (val) {
                    provider.toggleTheme();
                  },
                ),
              ),
              const SizedBox(width: 8),
            ],
          ),
        ],
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 440),
            child: Card(
              color: theme.colorScheme.surface, // Adapts to theme surface
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(24),
                side: BorderSide(color: theme.colorScheme.onSurface.withValues(alpha: 0.08)),
              ),
              elevation: 4,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 32.0),
                child: Form(
                  key: _formKey,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Header Brand
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.sync_alt, color: theme.colorScheme.primary, size: 28),
                          const SizedBox(width: 10),
                          Text(
                            "RuangKampus",
                            style: theme.textTheme.headlineMedium?.copyWith(
                              color: theme.colorScheme.onSurface,
                              fontWeight: FontWeight.bold,
                              letterSpacing: 0.8,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Center(
                        child: Text(
                          "Sistem Pemesanan Sarpras Real-Time",
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: theme.colorScheme.onSurface.withValues(alpha: 0.6),
                          ),
                        ),
                      ),
                      const SizedBox(height: 28),

                      // Tabs selector
                      Row(
                        children: [
                          Expanded(
                            child: ElevatedButton(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: _isLogin ? theme.colorScheme.primary : Colors.transparent,
                                foregroundColor: _isLogin ? Colors.white : theme.colorScheme.onSurface,
                                elevation: 0,
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                              onPressed: () => setState(() => _isLogin = true),
                              child: const Text("Masuk"),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: ElevatedButton(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: !_isLogin ? theme.colorScheme.primary : Colors.transparent,
                                foregroundColor: !_isLogin ? Colors.white : theme.colorScheme.onSurface,
                                elevation: 0,
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                              onPressed: () => setState(() => _isLogin = false),
                              child: const Text("Daftar"),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 24),

                      // Username Input (Styles explicitly use dark font on solid white fill for perfect accessibility)
                      TextFormField(
                        controller: _usernameController,
                        style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold),
                        decoration: _buildInputDecoration(
                          labelText: "Username",
                          icon: Icons.person_outline,
                          context: context,
                        ),
                        validator: (v) => v == null || v.trim().isEmpty ? "Masukkan username" : null,
                      ),
                      const SizedBox(height: 16),

                      // Password Input (Styles explicitly use dark font on solid white fill for perfect accessibility)
                      TextFormField(
                        controller: _passwordController,
                        obscureText: true,
                        style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold),
                        decoration: _buildInputDecoration(
                          labelText: "Sandi Rahasia",
                          icon: Icons.lock_outline,
                          context: context,
                        ),
                        validator: (v) => v == null || v.length < 4 ? "Minimal sandi 4 karakter" : null,
                      ),

                      if (!_isLogin) ...[
                        const SizedBox(height: 16),
                        // Full name Input (Styles explicitly use dark font on solid white fill for perfect accessibility)
                        TextFormField(
                          controller: _fullNameController,
                          style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold),
                          decoration: _buildInputDecoration(
                            labelText: "Nama Lengkap",
                            icon: Icons.badge_outlined,
                            context: context,
                          ),
                          validator: (v) => v == null || v.trim().isEmpty ? "Masukkan nama lengkap" : null,
                        ),
                        const SizedBox(height: 16),

                        // Role Selector Form fields
                        DropdownButtonFormField<String>(
                          initialValue: _selectedRole,
                          isExpanded: true,
                          dropdownColor: Colors.white,
                          style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 14),
                          decoration: _buildInputDecoration(
                            labelText: "Jabatan Peran",
                            icon: Icons.school_outlined,
                            context: context,
                          ),
                          items: const [
                            DropdownMenuItem(value: "student", child: Text("Mahasiswa Aktif", style: TextStyle(color: Colors.black))),
                            DropdownMenuItem(value: "lecturer", child: Text("Dosen Pengampu / Staff", style: TextStyle(color: Colors.black))),
                            DropdownMenuItem(value: "admin", child: Text("Administrator Sarpras", style: TextStyle(color: Colors.black))),
                          ],
                          onChanged: (v) => setState(() => _selectedRole = v!),
                        ),
                        const SizedBox(height: 14),

                        // Faculty / Dept Input
                        TextFormField(
                          controller: _facultyController,
                          style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold),
                          decoration: _buildInputDecoration(
                            labelText: "Fakultas / Unit",
                            icon: Icons.corporate_fare_outlined,
                            context: context,
                          ),
                          validator: (v) => v == null || v.trim().isEmpty ? "Masukkan fakultas/unit" : null,
                        ),
                      ],

                      const SizedBox(height: 28),

                      // Submit Button
                      ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: theme.colorScheme.primary,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                        onPressed: _submit,
                        child: Text(
                          _isLogin ? "MASUK SEKARANG" : "SINKRONISASI AKUN",
                          style: const TextStyle(fontWeight: FontWeight.bold, letterSpacing: 1),
                        ),
                      ),
                      const SizedBox(height: 20),

                      // Testing credentials guide popup inside login screen helper
                      if (_isLogin) ...[
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: theme.colorScheme.onSurface.withValues(alpha: 0.04),
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: theme.colorScheme.onSurface.withValues(alpha: 0.08)),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Icon(Icons.info_outline, color: theme.colorScheme.primary, size: 16),
                                  const SizedBox(width: 6),
                                  Text(
                                    "Kredensial Demo SQLite Intern:",
                                    style: TextStyle(color: theme.colorScheme.onSurface, fontSize: 10, fontWeight: FontWeight.bold),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 6),
                              _buildCredentialRow("Mahasiswa:", "mhs", "mhs123", theme),
                              _buildCredentialRow("Dosen Prioritas:", "dosen", "dosen123", theme),
                              _buildCredentialRow("Administrator:", "admin", "admin123", theme),
                            ],
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildCredentialRow(String label, String u, String p, ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 2.0),
      child: RichText(
        text: TextSpan(
          style: TextStyle(fontSize: 11, color: theme.colorScheme.onSurface),
          children: [
            TextSpan(text: "$label ", style: TextStyle(color: theme.colorScheme.onSurface.withValues(alpha: 0.6))),
            TextSpan(text: u, style: const TextStyle(color: Color(0xFFC2410C), fontWeight: FontWeight.bold)),
            const TextSpan(text: " / "),
            TextSpan(text: p, style: TextStyle(color: theme.colorScheme.onSurface, fontStyle: FontStyle.italic)),
          ],
        ),
      ),
    );
  }

  InputDecoration _buildInputDecoration({required String labelText, required IconData icon, required BuildContext context}) {
    final theme = Theme.of(context);
    return InputDecoration(
      labelText: labelText,
      labelStyle: const TextStyle(color: Colors.black54, fontSize: 13),
      prefixIcon: Icon(icon, color: Colors.black54, size: 18),
      filled: true,
      fillColor: Colors.white, // Always solid white so the entered text (black) is perfectly readable
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: Colors.black.withValues(alpha: 0.15)),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: theme.colorScheme.primary, width: 1.5),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.redAccent),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.redAccent, width: 1.5),
      ),
    );
  }
}
