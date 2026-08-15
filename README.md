# Tennant-Ari: Baby Shower Gallery

A beautiful, secure photo and video gallery application built for sharing memories of your baby shower celebration. This Spring Boot application allows you to create organized collections of memories and share them with family and friends using PIN-protected access.

## 🎉 Features

- **Photo & Video Gallery**: Upload and organize photos and videos from your baby shower
- **Collections**: Organize media into themed collections (gifts, decorations, guests, etc.)
- **Secure Access**: PIN-protected login to keep your special memories private
- **Automatic Thumbnails**: Automatic thumbnail generation for quick previews
- **Video Compression**: Videos are automatically compressed for faster loading
- **Responsive Design**: Works beautifully on desktop, tablet, and mobile devices
- **Google Drive Integration**: Backup and sync your memories to Google Drive
- **File Storage**: Secure local file storage with organized directory structure

## 🛠️ Tech Stack

- **Backend**: Spring Boot (Java)
- **Database**: JPA/Hibernate (default H2, configurable)
- **Frontend**: HTML5, CSS3, JavaScript
- **Security**: Spring Security with PIN-based authentication
- **Media Processing**: FFmpeg for video compression, image thumbnail generation
- **Cloud Integration**: Google Drive API

## 📋 Prerequisites

- Java 11 or higher
- Maven 3.6+
- FFmpeg (for video compression)
- Google Drive API credentials (optional, for cloud backup)

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/burnet01/Baby-Shower-ari.git
cd Baby-Shower-ari
```

### 2. Configure Application Properties
Edit `src/main/resources/application.properties`:
```properties
# Server Configuration
server.port=8080

# Google Drive (optional)
# google.drive.credentials=path/to/credentials.json

# File Storage
file.storage.location=uploads/
```

### 3. Build the Application
```bash
mvn clean package
```

### 4. Run the Application

**Option A: Using Maven**
```bash
mvn spring-boot:run
```

**Option B: Using Pre-built JAR**

Download the latest compiled JAR from [Releases](https://github.com/burnet01/Baby-Shower-ari/releases) and run:
```bash
java -jar gallery-1.0.0.jar
```

The application will start at `http://localhost:8080`

## 📱 Usage

1. **Login**: Enter your PIN to access the gallery
2. **Create Collections**: Organize memories by creating different collections
3. **Upload Media**: Add photos and videos to your collections
4. **Share**: Share the gallery with family and friends using the PIN
5. **View**: Browse and enjoy your baby shower memories

## 📂 Project Structure

```
src/main/java/com/tennantari/gallery/
├── controller/          # REST API endpoints
├── service/             # Business logic layer
├── repository/          # Data access layer
├── model/               # Entity models
└── config/              # Security & web configuration
src/main/resources/
├── templates/           # HTML templates
└── static/              # CSS and JavaScript
```

## 🔐 Security

- PIN-based authentication for privacy
- CSRF protection
- Secure session management
- Private file access

## 🎨 Customization

### Themes & Styling
Customize the look and feel by editing `src/main/resources/static/css/gallery.css`

### PIN Configuration
Configure PIN requirements in `src/main/java/com/tennantari/gallery/config/SecurityConfig.java`

## 🐛 Troubleshooting

### Videos not compressing
- Ensure FFmpeg is installed: `ffmpeg -version`
- Check file permissions on the upload directory

### Google Drive sync issues
- Verify API credentials are configured correctly
- Check internet connectivity

## � Releases

Compiled JAR files are available in [GitHub Releases](https://github.com/burnet01/Baby-Shower-ari/releases). Download the latest release and run it locally without needing Maven installed.

## �📝 License

[Add your license here]

## 👶 About

This application was created to make sharing and preserving memories from a special baby shower celebration easy and secure. Perfect for keeping family and friends connected during this exciting time!

## 🙏 Support

For questions or issues, please create an issue in the repository or contact the project maintainer.

---

Made with ❤️ for celebrating new beginnings

