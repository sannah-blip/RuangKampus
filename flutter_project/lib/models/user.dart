class User {
  final String username;
  final String passwordHash;
  final String fullName;
  final String role; // "admin", "student", "lecturer"
  final String faculty;

  User({
    required this.username,
    required this.passwordHash,
    required this.fullName,
    required this.role,
    this.faculty = "Fakultas Ilmu Komputer",
  });

  Map<String, dynamic> toMap() {
    return {
      'username': username,
      'passwordHash': passwordHash,
      'fullName': fullName,
      'role': role,
      'faculty': faculty,
    };
  }

  factory User.fromMap(Map<String, dynamic> map) {
    return User(
      username: map['username'] as String,
      passwordHash: map['passwordHash'] as String,
      fullName: map['fullName'] as String,
      role: map['role'] as String,
      faculty: map['faculty'] as String? ?? "Fakultas Ilmu Komputer",
    );
  }
}
