class Booking {
  final int? id;
  final int roomId;
  final String roomName;
  final String roomType;
  final String userName;
  final String userRole; // "student" or "lecturer" or "admin"
  final int startTime; // Epoch ms
  final int endTime; // Epoch ms
  final String dateString; // "YYYY-MM-DD"
  final String status; // "ACTIVE", "COMPLETED", "CANCELLED"
  final bool isPriorityOverride;
  final bool reminderSent;

  Booking({
    this.id,
    required this.roomId,
    required this.roomName,
    required this.roomType,
    required this.userName,
    required this.userRole,
    required this.startTime,
    required this.endTime,
    required this.dateString,
    this.status = "ACTIVE",
    this.isPriorityOverride = false,
    this.reminderSent = false,
  });

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'roomId': roomId,
      'roomName': roomName,
      'roomType': roomType,
      'userName': userName,
      'userRole': userRole,
      'startTime': startTime,
      'endTime': endTime,
      'dateString': dateString,
      'status': status,
      'isPriorityOverride': isPriorityOverride ? 1 : 0,
      'reminderSent': reminderSent ? 1 : 0,
    };
  }

  factory Booking.fromMap(Map<String, dynamic> map) {
    return Booking(
      id: map['id'] as int?,
      roomId: map['roomId'] as int,
      roomName: map['roomName'] as String,
      roomType: map['roomType'] as String,
      userName: map['userName'] as String,
      userRole: map['userRole'] as String,
      startTime: map['startTime'] as int,
      endTime: map['endTime'] as int,
      dateString: map['dateString'] as String,
      status: map['status'] as String? ?? "ACTIVE",
      isPriorityOverride: (map['isPriorityOverride'] as int) == 1,
      reminderSent: (map['reminderSent'] as int) == 1,
    );
  }

  Booking copyWith({
    int? id,
    int? roomId,
    String? roomName,
    String? roomType,
    String? userName,
    String? userRole,
    int? startTime,
    int? endTime,
    String? dateString,
    String? status,
    bool? isPriorityOverride,
    bool? reminderSent,
  }) {
    return Booking(
      id: id ?? this.id,
      roomId: roomId ?? this.roomId,
      roomName: roomName ?? this.roomName,
      roomType: roomType ?? this.roomType,
      userName: userName ?? this.userName,
      userRole: userRole ?? this.userRole,
      startTime: startTime ?? this.startTime,
      endTime: endTime ?? this.endTime,
      dateString: dateString ?? this.dateString,
      status: status ?? this.status,
      isPriorityOverride: isPriorityOverride ?? this.isPriorityOverride,
      reminderSent: reminderSent ?? this.reminderSent,
    );
  }
}
