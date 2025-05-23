# StackSight - Spring Boot Web Application

StackSight is a Stack Overflow-inspired web application built with Spring Boot, Thymeleaf, Bootstrap, and vanilla JavaScript. The application provides real-time insights into Stack Overflow trends using Hadoop framework.

## Features

- **User Authentication**: Sign-up, login, and logout functionality
- **Navigation**: Responsive navigation bar with dynamic menu items based on authentication status
- **Landing Page**: Modern hero section with curved design elements
- **Analytics Dashboard**: Visual representation of Stack Overflow trends and patterns
- **Search Functionality**: Advanced search with filters for tags, date, answer status, and language

## Technology Stack

- **Backend**: Spring Boot (with Spring Security)
- **Frontend**: 
  - Thymeleaf (Server-side templating)
  - Bootstrap 5 (Responsive UI framework)
  - JavaScript (Vanilla JS for interactivity)
- **Styling**: Custom CSS with a primary blue color (#2FA1FF)

## Project Structure

```
stacksight/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── stacksight/
│       │           ├── controllers/
│       │           ├── models/
│       │           ├── repositories/
│       │           ├── services/
│       │           └── StackSightApplication.java
│       └── resources/
│           ├── static/
│           │   ├── css/
│           │   │   └── styles.css
│           │   ├── js/
│           │   │   └── main.js
│           │   └── images/
│           └── templates/
│               ├── fragments/
│               │   ├── header.html
│               │   └── footer.html
│               ├── layout.html
│               ├── index.html
│               ├── login.html
│               └── signup.html
└── pom.xml
```

## Setup and Installation

1. Clone the repository
2. Configure application.properties with your database settings
3. Run the Spring Boot application
4. Access the application at http://localhost:8080

## UI Components

1. **Navigation Bar**: Contains logo, menu items, and authentication buttons
2. **Landing Page**: Showcases the application's main features
3. **Sign-in Page**: Allows users to log in with their credentials
4. **Sign-up Page**: Lets new users create an account
5. **Trends Dashboard**: Displays analytics with interactive charts

## Color Scheme

The primary color used throughout the application is #2FA1FF (blue), which is applied to buttons, links, and accent elements.

## Responsive Design

The application is fully responsive, adapting to different screen sizes from mobile to desktop using Bootstrap's grid system and responsive utilities.

## Future Enhancements

- Implementing the Trends dashboard with real data
- Adding user profile management
- Creating the Questions and Answers functionality
- Implementing the search with backend integration