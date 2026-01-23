package com.nabra.backend.common.model;

public class Enums {
  private Enums() {}

  public enum UserRole { USER, ADMIN }

  // User account status
  public enum UserStatus { ACTIVE, INACTIVE, SUSPENDED, DELETED }

  // Represents the user's accessibility / usage type from SRS (deaf, mute, hearing, instructor, etc.)
  public enum UserType { DEAF, MUTE, HEARING, INSTRUCTOR, OTHER }

  public enum SessionOutputType { TEXT, VOICE }

  public enum SessionInputType { LIVE, RECORDED }

  public enum SessionStatus { ACTIVE, COMPLETED, FAILED }

  public enum MessageType { TEXT, IMAGE, FILE, VOICE }

  public enum DeliveryStatus { SENT, DELIVERED, READ }

  public enum LearningLevel { BEGINNER, INTERMEDIATE, ADVANCED }

  public enum ReportStatus { OPEN, UNDER_REVIEW, RESOLVED, REJECTED }
}
