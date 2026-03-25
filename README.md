🔍 Deepfake Detection Web App (VeraLens)

A full-stack web application that analyzes images to detect potential deepfake manipulations using rule-based forensic techniques.

---

🚀 Live Demo

👉 https://heyyydevv.github.io/deepfake-detector/

---

🧠 Features

- 📸 Upload image for analysis
- ⚡ Real-time detection using backend API
- 🧪 Rule-based forensic checks:
  - Pixel Intensity Variance
  - Blur / Smoothing Detection
  - Edge Sharpness Inconsistency
  - Facial Symmetry Analysis
- 📊 Confidence score display
- 🎨 Clean and modern UI

---

🏗️ Tech Stack

Frontend

- HTML
- CSS
- JavaScript

Backend

- Java (Spring Boot)
- REST API

Deployment

- GitHub Pages (Frontend)

---

⚙️ How It Works

1. User uploads an image
2. Frontend sends image to backend API
3. Backend analyzes image using rule-based logic
4. Response returned with result + confidence score
5. UI updates dynamically

---

📂 Project Structure

deepfake-detector/
│
├── index.html
├── style.css
├── script.js
│
└── backend/
    ├── controller/
    ├── service/
    └── model/

---

▶️ Run Locally

1. Clone the repository

git clone https://github.com/heyyydevv/deepfake-detector.git

2. Backend setup

cd backend
mvn spring-boot:run

3. Open frontend

Open "index.html" in browser

---

💡 Future Improvements

- 🤖 Integrate ML-based deepfake detection
- ☁️ Deploy backend to cloud
- 📁 Support video deepfake detection
- 🔐 Add authentication system

---

👨‍💻 Author

Devraj Singh
📸 Instagram: @heeyyydevv

---

⭐ Show Your Support

If you like this project, give it a ⭐ on GitHub!
