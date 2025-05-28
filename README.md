# AnorBooking Project Overview
AnorBooking is a comprehensive event booking and management system built with Spring Boot. This is a full-featured web application that allows users to discover, book, and manage event tickets.

## 🎯 Core Purpose
The project serves as an event ticketing platform where:

- Event organizers can create and manage events
- Users can browse, search, and book tickets for events
- Administrators can oversee the entire platform

## 🏗️ Technical Architecture
### Technology Stack
- Framework : Spring Boot 3.4.5 with Java 17
- Database : PostgreSQL with JPA/Hibernate
- Security : Spring Security with JWT authentication
- Documentation : Swagger/OpenAPI
- Email : Spring Mail for notifications
- Caching : Spring Cache
- Rate Limiting : Bucket4j
- Template Engine : Thymeleaf

### Key Features 
🎫 Event Management
- Create and publish events with categories
- Set pricing, dates, locations, and descriptions
- Upload event images
- Manage different ticket types 👥 User Management
- User registration and authentication
- Role-based access (User, Organizer, Admin)
- Profile management
- OTP verification system

💳 Booking System
- Event discovery and search
- Ticket booking with quantity selection
- Booking reference generation
- Payment processing integration
- Booking status tracking

📊 Administrative Features
- Admin dashboard
- User management
- Organizer application approval
- Audit logging
- Statistics and reporting
- Event analytics
  
🔔 Communication
- Email notifications
- In-app notifications
- Event reminders (scheduled)
- Password reset functionality 🛡️ Security & Performance
- JWT-based authentication
- Rate limiting to prevent abuse
- Input validation
- Global exception handling
- Audit trail for important actions
## 📁 Project Structure
The project follows a clean, layered architecture:

- Controllers : REST API endpoints
- Services : Business logic
- Repositories : Data access layer
- Models : JPA entities
- DTOs : Data transfer objects
- Security : Authentication and authorization
- Config : Application configuration
- Utils : Helper utilities

## 🚀 Key Capabilities
1. Multi-role system supporting regular users, event organizers, and administrators
2. Comprehensive event lifecycle from creation to booking to analytics
3. Scalable architecture with caching, rate limiting, and async processing
4. Security-first approach with JWT tokens and role-based permissions
5. Audit and reporting capabilities for compliance and insights
