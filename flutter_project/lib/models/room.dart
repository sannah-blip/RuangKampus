class Room {
  final int? id;
  final String name;
  final String type; // "ROOM" or "DESK"
  final int totalSeats;
  final int availableSeats;
  final String location;
  final String facilities;
  final bool priorityOnly;
  final bool isUnderMaintenance;

  Room({
    this.id,
    required this.name,
    required this.type,
    required this.totalSeats,
    required this.availableSeats,
    required this.location,
    required this.facilities,
    this.priorityOnly = false,
    this.isUnderMaintenance = false,
  });

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'name': name,
      'type': type,
      'totalSeats': totalSeats,
      'availableSeats': availableSeats,
      'location': location,
      'facilities': facilities,
      'priorityOnly': priorityOnly ? 1 : 0,
      'isUnderMaintenance': isUnderMaintenance ? 1 : 0,
    };
  }

  factory Room.fromMap(Map<String, dynamic> map) {
    return Room(
      id: map['id'] as int?,
      name: map['name'] as String,
      type: map['type'] as String,
      totalSeats: map['totalSeats'] as int,
      availableSeats: map['availableSeats'] as int,
      location: map['location'] as String,
      facilities: map['facilities'] as String,
      priorityOnly: (map['priorityOnly'] as int) == 1,
      isUnderMaintenance: (map['isUnderMaintenance'] as int) == 1,
    );
  }

  Room copyWith({
    int? id,
    String? name,
    String? type,
    int? totalSeats,
    int? availableSeats,
    String? location,
    String? facilities,
    bool? priorityOnly,
    bool? isUnderMaintenance,
  }) {
    return Room(
      id: id ?? this.id,
      name: name ?? this.name,
      type: type ?? this.type,
      totalSeats: totalSeats ?? this.totalSeats,
      availableSeats: availableSeats ?? this.availableSeats,
      location: location ?? this.location,
      facilities: facilities ?? this.facilities,
      priorityOnly: priorityOnly ?? this.priorityOnly,
      isUnderMaintenance: isUnderMaintenance ?? this.isUnderMaintenance,
    );
  }
}
