import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';
import 'providers/ruang_kampus_provider.dart';
import 'screens/auth_screen.dart';
import 'screens/dashboard_screen.dart';

void main() async {
  // Ensure that Flutter widget binding is initialized before launching
  WidgetsFlutterBinding.ensureInitialized();
  
  runApp(const RuangKampusApp());
}

class RuangKampusApp extends StatelessWidget {
  const RuangKampusApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => RuangKampusProvider(),
      child: Consumer<RuangKampusProvider>(
        builder: (context, provider, _) {
          final isDark = provider.isDarkMode;

          final lightTheme = ThemeData(
            useMaterial3: true,
            colorScheme: ColorScheme.fromSeed(
              seedColor: const Color(0xFF4F46E5), // Indigo accents
              primary: const Color(0xFF4F46E5),   // Vivid Indigo
              secondary: const Color(0xFF10B981), // Emerald Teal
              brightness: Brightness.light,
              background: const Color(0xFFF9FAFB), // Clean off-white bg
              surface: const Color(0xFFFFFFFF),    // Pure white sheet surface
              onBackground: const Color(0xFF000000), // Pure black text
              onSurface: const Color(0xFF111827),    // Dark Charcoal black
            ),
            scaffoldBackgroundColor: const Color(0xFFF3F4F6), // Light grey backdrop
            textTheme: GoogleFonts.plusJakartaSansTextTheme(
              ThemeData.light().textTheme.copyWith(
                headlineMedium: GoogleFonts.plusJakartaSans(
                  fontWeight: FontWeight.bold,
                  color: Colors.black,
                ),
                titleLarge: GoogleFonts.plusJakartaSans(
                  fontWeight: FontWeight.w600,
                  color: Colors.black,
                ),
                bodyMedium: GoogleFonts.plusJakartaSans(
                  color: Colors.black,
                ),
                bodyLarge: GoogleFonts.plusJakartaSans(
                  color: Colors.black,
                ),
              ),
            ),
          );

          final darkTheme = ThemeData(
            useMaterial3: true,
            colorScheme: ColorScheme.fromSeed(
              seedColor: const Color(0xFF6366F1), // Indigo accents
              primary: const Color(0xFF6366F1),   // Vivid Indigo
              secondary: const Color(0xFF10B981), // Emerald Teal
              brightness: Brightness.dark,
              background: const Color(0xFF131824), // Slate backdrop
              surface: const Color(0xFF1E2638),    // Container slate
              onBackground: const Color(0xFFFFFFFF), // White text
              onSurface: const Color(0xFFF3F4F6),    // Dark background white
            ),
            scaffoldBackgroundColor: const Color(0xFF131824),
            textTheme: GoogleFonts.plusJakartaSansTextTheme(
              ThemeData.dark().textTheme.copyWith(
                headlineMedium: GoogleFonts.plusJakartaSans(
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
                titleLarge: GoogleFonts.plusJakartaSans(
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
                bodyMedium: GoogleFonts.plusJakartaSans(
                  color: const Color(0xFFE5E7EB),
                ),
                bodyLarge: GoogleFonts.plusJakartaSans(
                  color: Colors.white,
                ),
              ),
            ),
          );

          return MaterialApp(
            title: 'RuangKampus',
            debugShowCheckedModeBanner: false,
            theme: isDark ? darkTheme : lightTheme,
            home: provider.currentUser == null
                ? const AuthScreen()
                : const DashboardScreen(),
          );
        },
      ),
    );
  }
}
